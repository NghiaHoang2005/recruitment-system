package com.recruitment.backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.recruitment.backend.domain.dtos.Cv.CvUploadCompleteRequest;
import com.recruitment.backend.domain.entities.Candidate;
import com.recruitment.backend.domain.entities.Cv;
import com.recruitment.backend.domain.entities.CvStatus;
import com.recruitment.backend.repositories.CandidateRepository;
import com.recruitment.backend.repositories.CvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CvService {
    private final Cloudinary cloudinary;
    private final AsyncCvProcessor asyncCvProcessor;
    private final CvRepository cvRepository;
    private final CandidateRepository candidateRepository;

    public Cv processAndSaveUploadedCv(UUID currentUserId, CvUploadCompleteRequest request) {
        try {
            Map resourceData = cloudinary.api().resource(request.getPublicId(), ObjectUtils.emptyMap());
            Map contextData = (Map) resourceData.get("context");
            Map customData = (Map) contextData.get("custom");
            String uploaderId = (String) customData.get("uploader_id");

            if (uploaderId == null || !uploaderId.equals(currentUserId.toString())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hành vi gian lận bị từ chối!");
            }

            Candidate candidateRef = candidateRepository.getReferenceById(currentUserId);

            Cv newCv = new Cv();
            newCv.setFileUrl(request.getPublicId());
            newCv.setCvName(request.getCvName());
            newCv.setAiStatus(CvStatus.PENDING);
            newCv.setCandidate(candidateRef);

            newCv = cvRepository.save(newCv);

            asyncCvProcessor.processCvInBackground(newCv.getId(), request.getPublicId());

            return newCv;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi xử lý CV ID trên Cloudinary: {}", request.getPublicId(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File không tồn tại trên hệ thống!");
        }
    }

    public Map<String, Object> getExtractedData(UUID currentUserId, UUID cvId) {
        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV không tồn tại"));

        if (!cv.getCandidate().getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập CV này");
        }

        if (CvStatus.PENDING.equals(cv.getAiStatus())) {
            throw new ResponseStatusException(HttpStatus.ACCEPTED, "AI is still processing");
        } else if (CvStatus.FAILED.equals(cv.getAiStatus())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi trích xuất CV");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("cvId", cv.getId());
        response.put("parsedData", cv.getParsedData());

        return response;
    }
}
