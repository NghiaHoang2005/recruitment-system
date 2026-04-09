package com.recruitment.backend.services;

import com.recruitment.backend.domain.entities.Cv;
import com.recruitment.backend.domain.entities.CvStatus;
import com.recruitment.backend.repositories.CvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncCvProcessor {
    private final IntegrationService integrationService;
    private final CvRepository cvRepository;

    @Async("taskExecutor")
    public void processCvInBackground(Long cvId, String fileUrl) {
        try {
            String rawText = integrationService.extractTextFromUrl(fileUrl);
            String jsonResult = integrationService.callAiToParseJson(rawText);
            Cv cv = cvRepository.findById(cvId).orElseThrow();
            cv.setParsedData(jsonResult);
            cv.setAiStatus(CvStatus.COMPLETED);
            cvRepository.save(cv);
            log.info("Xử lý thành công CV ID: {}", cvId);

        } catch (Exception e) {
            log.error("Lỗi khi xử lý CV ID: {}", cvId, e);
            Cv cv = cvRepository.findById(cvId).orElseThrow();
            cv.setAiStatus(CvStatus.FAILED);
            cvRepository.save(cv);
        }
    }
}
