package com.recruitment.backend.services;

import com.cloudinary.Cloudinary;
import com.recruitment.backend.domain.dtos.Cv.CvItemResponse;
import com.recruitment.backend.domain.dtos.Cv.CvUploadRequest;
import com.recruitment.backend.domain.dtos.Cv.ExtractionStatusResponse;
import com.recruitment.backend.domain.dtos.CvResponse;
import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.CvStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CandidateRepository;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.services.storage.FirebaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CvService {
    private final Cloudinary cloudinary;
    private final AsyncCvProcessor asyncCvProcessor;
    private final CvRepository cvRepository;
    private final CandidateRepository candidateRepository;
    private final FirebaseStorageService firebaseStorageService;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    private void validateCvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null ||
                (!originalFilename.toLowerCase().endsWith(".pdf") &&
                        !originalFilename.toLowerCase().endsWith(".docx"))) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    public CvResponse processAndSaveUploadedCv(UUID currentUserId, CvUploadRequest request) {
       try{
           validateCvFile(request.getFile());

           String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM"));
           String folder = "cv_uploads/" + currentMonth;
           String filePath = firebaseStorageService.uploadCv(request.getFile(), folder);

           boolean hasNoCv = cvRepository.findByCandidateUserIdOrderByIsDefaultDescUploadedAtDesc(currentUserId).isEmpty();

           Candidate candidateRef = candidateRepository.getReferenceById(currentUserId);
           Cv newCv = new Cv();
            newCv.setFileUrl(filePath);
            newCv.setCvName(request.getFile().getOriginalFilename());
            newCv.setAiStatus(CvStatus.PENDING);
            newCv.setIsDefault(hasNoCv);
            newCv.setCandidate(candidateRef);
            newCv = cvRepository.save(newCv);

           String signedUrlForAi = firebaseStorageService.getPresignedUrl(filePath);
           asyncCvProcessor.processCvInBackground(newCv.getId(), signedUrlForAi);

           return CvResponse.builder()
                   .id(newCv.getId())
                   .fileName(newCv.getCvName())
                   .fileUrl(filePath)
                   .uploadedAt(newCv.getUploadedAt())
                   .build();

       } catch (Exception e) {
           log.error("Lỗi khi upload CV: ", e);
           throw new AppException(ErrorCode.CV_PROCESSING_FAILED);
       }
    }

    public Map<String, Object> getExtractedData(UUID currentUserId, UUID cvId) {
        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));

        if (!cv.getCandidate().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (CvStatus.PENDING.equals(cv.getAiStatus())) {
            throw new AppException(ErrorCode.AI_PROCESSING);
        } else if (CvStatus.FAILED.equals(cv.getAiStatus())) {
            throw new AppException(ErrorCode.CV_PROCESSING_FAILED);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("cvId", cv.getId());
        response.put("parsedData", cv.getParsedData());

        return response;
    }

    public String getPresignedUrl(UUID currentUserId, UUID cvId) {
        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));

        if (!cv.getCandidate().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        try {
            String signedUrl = firebaseStorageService.getPresignedUrl(cv.getFileUrl());

            return signedUrl;

        } catch (Exception e) {
            log.error("Lỗi khi tạo Presigned URL từ Cloudinary cho file: {}", cv.getFileUrl(), e);
            throw new AppException(ErrorCode.PRESIGNED_URL_FAILED);
        }
    }

    @Transactional
    public void retryCvExtraction(UUID currentUserId, UUID cvId) {
        log.info("Starting CV retry extraction for cvId: {} by user: {}", cvId, currentUserId);

        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));

        if (!cv.getCandidate().getUserId().equals(currentUserId)) {
            log.warn("Unauthorized retry attempt for cvId: {} by user: {}", cvId, currentUserId);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Only allow reextraction when status is FAILED
        if (!CvStatus.FAILED.equals(cv.getAiStatus())) {
            log.error("Cannot retry extraction - CV {} status is {}, must be FAILED", cvId, cv.getAiStatus());
            throw new AppException(ErrorCode.CV_PROCESSING_FAILED);
        }

        if (cv.getFileUrl() == null || cv.getFileUrl().isBlank()) {
            log.error("CV {} has no file URL", cvId);
            throw new AppException(ErrorCode.CV_NOT_FOUND);
        }

        Integer currentRetryCount = (cv.getRetryCount() != null ? cv.getRetryCount() : 0);
        cv.setRetryCount(currentRetryCount + 1);
        cv.setLastRetryAt(LocalDateTime.now());

        cv.setAiStatus(CvStatus.PENDING);
        cv.setRawText(null);
        cv.setParsedData(null);
        cvRepository.save(cv);
        log.debug("Cleared previous extraction data for CV: {}, retry count: {}", cvId, cv.getRetryCount());

        try {
            String signedUrl = firebaseStorageService.getPresignedUrl(cv.getFileUrl());
            log.debug("Generated presigned URL for CV retry: {}", cvId);

            asyncCvProcessor.processCvInBackground(cvId, signedUrl);
            log.info("✓ CV retry extraction triggered for cvId: {}, attempt #{}", cvId, cv.getRetryCount());

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for CV retry: {}", cvId, e);
            cv.setAiStatus(CvStatus.FAILED);
            cvRepository.save(cv);
            throw new AppException(ErrorCode.PRESIGNED_URL_FAILED);
        }
    }

    public ExtractionStatusResponse getExtractionStatus(UUID currentUserId, UUID cvId) {
        log.debug("Getting extraction status for cvId: {} by user: {}", cvId, currentUserId);

        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));

        if (!cv.getCandidate().getUserId().equals(currentUserId)) {
            log.warn("Unauthorized status check attempt for cvId: {} by user: {}", cvId, currentUserId);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ExtractionStatusResponse response = ExtractionStatusResponse.builder()
                .cvId(cv.getId())
                .status(cv.getAiStatus())
                .build();

        if (CvStatus.COMPLETED.equals(cv.getAiStatus())) {
            response.setParsedData(cv.getParsedData());
        } else if (CvStatus.FAILED.equals(cv.getAiStatus())) {
            // Could add error message if we stored it in the future
            response.setErrorMessage("CV extraction failed");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<CvItemResponse> getCurrentUserCv(UUID currentUserId) {
        List<Cv> cvs = cvRepository.findByCandidateUserIdOrderByIsDefaultDescUploadedAtDesc(currentUserId);

        return cvs.stream()
                .map(cv -> CvItemResponse.builder()
                        .id(cv.getId())
                        .cvName(cv.getCvName())
                        .build())
                .toList();
    }

    @Transactional
    public void setDefaultCv(UUID currentUserId, UUID cvId) {
        Cv cv = cvRepository.findByIdAndCandidateUserId(cvId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));

        cvRepository.clearDefaultByCandidateId(currentUserId);
        cv.setIsDefault(true);
        cvRepository.save(cv);

    }

    @Transactional
    public void deleteCv(UUID currentUserId, UUID cvId) {
        Cv cv = cvRepository.findByIdAndCandidateUserId(cvId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));

        boolean wasDefault = Boolean.TRUE.equals(cv.getIsDefault());

        cvRepository.delete(cv);

        if (wasDefault) {
            cvRepository.findTopByCandidateUserIdOrderByUploadedAtDesc(currentUserId)
                    .ifPresent(nextCv -> {
                        nextCv.setIsDefault(true);
                        cvRepository.save(nextCv);
                    });
        }

        try {
            firebaseStorageService.deleteFile(cv.getFileUrl());
        } catch (Exception e) {
            log.error("Lỗi khi xóa file CV từ Firebase Storage: {}", cv.getFileUrl(), e);
        }
    }
}
