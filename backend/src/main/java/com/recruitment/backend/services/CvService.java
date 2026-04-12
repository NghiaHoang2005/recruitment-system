package com.recruitment.backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.recruitment.backend.domain.dtos.Cv.CvUploadRequest;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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
           String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM"));
           String folder = "cv_uploads/" + currentMonth;
           String filePath = firebaseStorageService.uploadCv(request.getFile(), folder);

           Candidate candidateRef = candidateRepository.getReferenceById(currentUserId);
           Cv newCv = new Cv();
           newCv.setFileUrl(filePath);
           newCv.setCvName(request.getFile().getOriginalFilename());
           newCv.setAiStatus(CvStatus.PENDING);
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
}
