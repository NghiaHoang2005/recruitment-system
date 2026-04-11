package com.recruitment.backend.services.ai.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.CvEmbedding;
import com.recruitment.backend.domain.entities.Cv.EmbeddingType;
import com.recruitment.backend.repositories.CvEmbeddingRepository;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.EmbeddingRequest;
import com.recruitment.backend.services.ai.model.EmbeddingResult;
import com.recruitment.backend.services.ai.providers.EmbeddingProvider;
import com.recruitment.backend.services.ai.providers.ProviderRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CvEmbeddingPipelineService {

    private static final String STEP_NAME = "EMBEDDING";

    private final CvRepository cvRepository;
    private final CvEmbeddingRepository cvEmbeddingRepository;
    private final ProviderRegistry providerRegistry;
    private final AiProperties aiProperties;
    private final AiRunLoggingService aiRunLoggingService;
    private final ObjectMapper objectMapper;

    public CvEmbeddingPipelineService(
            CvRepository cvRepository,
            CvEmbeddingRepository cvEmbeddingRepository,
            ProviderRegistry providerRegistry,
            AiProperties aiProperties,
            AiRunLoggingService aiRunLoggingService,
            ObjectMapper objectMapper
    ) {
        this.cvRepository = cvRepository;
        this.cvEmbeddingRepository = cvEmbeddingRepository;
        this.providerRegistry = providerRegistry;
        this.aiProperties = aiProperties;
        this.aiRunLoggingService = aiRunLoggingService;
        this.objectMapper = objectMapper;
    }

    public void embedAndStore(
            UUID cvId,
            String rawText,
            String normalizedText,
            String parsedJson,
            String language,
            List<String> rawChunks
    ) {
        String requestId = cvId.toString();
        Cv cv = cvRepository.getReferenceById(cvId);
        cvEmbeddingRepository.deleteByCvId(cvId);

        List<EmbeddingInput> inputs = new ArrayList<>();
        int chunkIndex = 0;
        for (String chunk : rawChunks) {
            inputs.add(new EmbeddingInput(EmbeddingType.RAW, chunk, chunkIndex++));
        }

        String summary = extractSummary(parsedJson);
        if (!summary.isBlank()) {
            inputs.add(new EmbeddingInput(EmbeddingType.SUMMARY, summary, 0));
        }

        String skills = extractSkills(parsedJson);
        if (!skills.isBlank()) {
            inputs.add(new EmbeddingInput(EmbeddingType.SKILLS, skills, 0));
        }

        String experience = extractExperience(parsedJson);
        if (!experience.isBlank()) {
            inputs.add(new EmbeddingInput(EmbeddingType.EXPERIENCE, experience, 0));
        }

        if (inputs.isEmpty()) {
            return;
        }

        EmbeddingProvider provider = providerRegistry.getEmbeddingProvider();
        List<String> texts = inputs.stream().map(EmbeddingInput::text).toList();
        long start = System.currentTimeMillis();

        // Use recommended dimensions based on provider and model
        Integer effectiveDimensions = aiProperties.getEmbedding().getRecommendedDimensions();
        
        EmbeddingResult result = provider.embed(EmbeddingRequest.builder()
                .texts(texts)
                .model(aiProperties.getEmbedding().getModel())
                .dimensions(effectiveDimensions)
                .timeoutMs(aiProperties.getEmbedding().getTimeoutMs())
                .build());

        long latency = System.currentTimeMillis() - start;
        List<float[]> vectors = result.getVectors();
        String promptVersion = aiProperties.getPrompts().getActiveVersion();

        for (int i = 0; i < inputs.size(); i++) {
            EmbeddingInput input = inputs.get(i);
            CvEmbedding embedding = CvEmbedding.builder()
                    .cv(cv)
                    .type(input.type())
                    .content(input.text())
                    .model(result.getModelName())
                    .promptVersion(promptVersion)
                    .dimensions(result.getDimensions())
                    .language(language)
                    .chunkIndex(input.chunkIndex())
                    .tokenCount(approxTokenCount(input.text()))
                    .vector(vectors.get(i))
                    .build();
            cvEmbeddingRepository.save(embedding);
        }

        aiRunLoggingService.logSuccess(
                cvId,
                requestId,
                STEP_NAME,
                result.getProvider(),
                result.getModelName(),
                result.getModelVersion(),
                promptVersion,
                result.getUsage().getInputTokens(),
                result.getUsage().getOutputTokens(),
                latency
        );
    }

    private String extractSummary(String parsedJson) {
        try {
            JsonNode root = objectMapper.readTree(parsedJson);
            return root.path("summary").asText("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractSkills(String parsedJson) {
        try {
            JsonNode root = objectMapper.readTree(parsedJson);
            List<String> skills = new ArrayList<>();
            for (JsonNode node : root.path("extracted_skills")) {
                skills.add(node.asText());
            }
            return String.join(", ", skills);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractExperience(String parsedJson) {
        try {
            JsonNode root = objectMapper.readTree(parsedJson);
            List<String> lines = new ArrayList<>();
            for (JsonNode exp : root.path("experiences")) {
                String line = String.join(" | ",
                        exp.path("company").asText(""),
                        exp.path("title").asText(""),
                        exp.path("description").asText(""));
                lines.add(line);
            }
            return String.join("\n", lines);
        } catch (Exception ignored) {
            return "";
        }
    }

    private int approxTokenCount(String text) {
        return Math.max(1, text.length() / 4);
    }

    private record EmbeddingInput(EmbeddingType type, String text, int chunkIndex) {
    }
}
