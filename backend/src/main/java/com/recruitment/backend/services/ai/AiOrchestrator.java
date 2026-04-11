package com.recruitment.backend.services.ai;

import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.CvStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.services.ai.pipeline.CvEmbeddingPipelineService;
import com.recruitment.backend.services.ai.pipeline.CvStructuredExtractionService;
import com.recruitment.backend.services.ai.pipeline.CvTextExtractionService;
import com.recruitment.backend.services.ai.pipeline.TextNormalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiOrchestrator {

    private final CvRepository cvRepository;
    private final CvTextExtractionService cvTextExtractionService;
    private final TextNormalizationService textNormalizationService;
    private final CvStructuredExtractionService cvStructuredExtractionService;
    private final CvEmbeddingPipelineService cvEmbeddingPipelineService;

    public void processCv(UUID cvId, String signedUrl) {
        Cv cv = cvRepository.findById(cvId).orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));
        cv.setAiStatus(CvStatus.PENDING);
        cvRepository.save(cv);

        String rawText = cvTextExtractionService.extractTextFromSignedUrl(cvId, signedUrl);
        String normalizedText = textNormalizationService.normalize(rawText);
        List<String> chunks = textNormalizationService.chunkByApproxTokens(normalizedText);
        String language = textNormalizationService.detectLanguage(normalizedText);

        String parsedJson = cvStructuredExtractionService.extract(cvId, normalizedText, language);

        cv.setRawText(rawText);
        cv.setParsedData(parsedJson);
        cvRepository.save(cv);

        cvEmbeddingPipelineService.embedAndStore(cvId, rawText, normalizedText, parsedJson, language, chunks);
    }
}
