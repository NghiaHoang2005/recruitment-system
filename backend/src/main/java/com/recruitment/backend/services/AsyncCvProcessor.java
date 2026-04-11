package com.recruitment.backend.services;

import com.cloudinary.Cloudinary;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.CvStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.services.ai.AiOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncCvProcessor {
    private final AiOrchestrator aiOrchestrator;
    private final CvRepository cvRepository;
    private final Cloudinary cloudinary;

    @Async("taskExecutor")
    public void processCvInBackground(UUID cvId, String publicId) {
        try {
            String signedUrl = cloudinary.url()
                    .resourceType("raw")
                    .type("authenticated")
                    .signed(true)
                    .generate(publicId);
            aiOrchestrator.processCv(cvId, signedUrl);

            Cv cv = cvRepository.findById(cvId).orElseThrow(()->new AppException(ErrorCode.CV_NOT_FOUND));
            cv.setAiStatus(CvStatus.COMPLETED);
            cvRepository.save(cv);
            log.info("Xử lý thành công CV ID: {}", cvId);

        } catch (Exception e) {
            log.error("Lỗi khi xử lý CV ID: {}", cvId, e);
            Cv cv = cvRepository.findById(cvId).orElseThrow(()->new AppException(ErrorCode.CV_NOT_FOUND));
            cv.setAiStatus(CvStatus.FAILED);
            cvRepository.save(cv);
        }
    }
}
