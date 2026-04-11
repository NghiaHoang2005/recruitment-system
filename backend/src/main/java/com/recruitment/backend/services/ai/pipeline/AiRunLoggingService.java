package com.recruitment.backend.services.ai.pipeline;

import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.CvAiRun;
import com.recruitment.backend.repositories.CvAiRunRepository;
import com.recruitment.backend.repositories.CvRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiRunLoggingService {

    private final CvAiRunRepository cvAiRunRepository;
    private final CvRepository cvRepository;

    public void logSuccess(
            UUID cvId,
            String requestId,
            String step,
            String provider,
            String modelName,
            String modelVersion,
            String promptVersion,
            Integer inputTokens,
            Integer outputTokens,
            Long latencyMs
    ) {
        Cv cv = cvRepository.getReferenceById(cvId);
        cvAiRunRepository.save(CvAiRun.builder()
                .cv(cv)
                .requestId(requestId)
                .step(step)
                .provider(provider)
                .modelName(modelName)
                .modelVersion(modelVersion)
                .promptVersion(promptVersion)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .latencyMs(latencyMs)
                .status("SUCCESS")
                .build());
    }

    public void logFailure(
            UUID cvId,
            String requestId,
            String step,
            String provider,
            String modelName,
            String modelVersion,
            String promptVersion,
            String errorMessage,
            Long latencyMs
    ) {
        Cv cv = cvRepository.getReferenceById(cvId);
        cvAiRunRepository.save(CvAiRun.builder()
                .cv(cv)
                .requestId(requestId)
                .step(step)
                .provider(provider)
                .modelName(modelName)
                .modelVersion(modelVersion)
                .promptVersion(promptVersion)
                .latencyMs(latencyMs)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build());
    }
}
