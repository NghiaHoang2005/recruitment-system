package com.recruitment.backend.services;

import com.cloudinary.Cloudinary;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.CvStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncCvProcessor {
    private final IntegrationService integrationService;
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
            String rawText = integrationService.extractTextFromUrl(signedUrl);
            String jsonResult = integrationService.callAiToParseJson(rawText);
            Cv cv = cvRepository.findById(cvId).orElseThrow(()->new AppException(ErrorCode.CV_NOT_FOUND));
            cv.setParsedData(jsonResult);
            cv.setAiStatus(CvStatus.COMPLETED);
            cv.setRawText(rawText);
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
