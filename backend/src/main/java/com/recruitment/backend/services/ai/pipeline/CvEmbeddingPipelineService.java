package com.recruitment.backend.services.ai.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.CvEmbedding;
import com.recruitment.backend.domain.entities.Cv.EmbeddingType;
import com.recruitment.backend.repositories.CvEmbeddingRepository;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.services.ai.config.AiConfigLoader;
import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.EmbeddingRequest;
import com.recruitment.backend.services.ai.model.EmbeddingResult;
import com.recruitment.backend.services.ai.providers.EmbeddingProvider;
import com.recruitment.backend.services.ai.providers.PromptTemplateProvider;
import com.recruitment.backend.services.ai.providers.ProviderRegistry;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CvEmbeddingPipelineService {

    private static final String STEP_NAME = "EMBEDDING";

    private final CvRepository cvRepository;
    private final CvEmbeddingRepository cvEmbeddingRepository;
    private final ProviderRegistry providerRegistry;
    private final AiProperties aiProperties;
    private final AiRunLoggingService aiRunLoggingService;
    private final ObjectMapper objectMapper;
    private final AiConfigLoader aiConfigLoader;
    private final PromptTemplateProvider promptTemplateProvider;

    public CvEmbeddingPipelineService(
            CvRepository cvRepository,
            CvEmbeddingRepository cvEmbeddingRepository,
            ProviderRegistry providerRegistry,
            AiProperties aiProperties,
            AiRunLoggingService aiRunLoggingService,
            ObjectMapper objectMapper,
            AiConfigLoader aiConfigLoader,
            PromptTemplateProvider promptTemplateProvider
    ) {
        this.cvRepository = cvRepository;
        this.cvEmbeddingRepository = cvEmbeddingRepository;
        this.providerRegistry = providerRegistry;
        this.aiProperties = aiProperties;
        this.aiRunLoggingService = aiRunLoggingService;
        this.objectMapper = objectMapper;
        this.aiConfigLoader = aiConfigLoader;
        this.promptTemplateProvider = promptTemplateProvider;
    }
    @Transactional
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
        
        log.debug("Starting embedding generation for CV: {} (language: {})", cvId, language);
        
        cvEmbeddingRepository.deleteByCvId(cvId);

        // Create embedding inputs from all 4 types
        List<EmbeddingInput> inputs = new ArrayList<>();
        
        // Type 1: RAW - text chunks (each chunk is a separate embedding)
        int chunkIndex = 0;
        for (String chunk : rawChunks) {
            inputs.add(new EmbeddingInput(
                    EmbeddingType.RAW,
                    buildEmbeddingText("cv_embed_raw", language, rawText, chunk, parsedJson),
                    chunkIndex++
            ));
        }
        log.debug("Added {} RAW embeddings (text chunks) for CV: {}", chunkIndex, cvId);

        // Type 2: SUMMARY - professional summary
        String summary = extractSummary(parsedJson);
        if (summary.isBlank()) {
            summary = fallbackSummary(language);
            log.debug("SUMMARY is empty, using fallback text for CV: {}", cvId);
        }
        inputs.add(new EmbeddingInput(
                EmbeddingType.SUMMARY,
                buildEmbeddingText("cv_embed_summary", language, rawText, summary, parsedJson),
                0
        ));
        log.debug("Added SUMMARY embedding for CV: {} ({} chars)", cvId, summary.length());

        // Type 3: SKILLS - consolidated skills
        String skills = extractSkills(parsedJson);
        if (skills.isBlank()) {
            skills = fallbackSkills(language);
            log.debug("SKILLS is empty, using fallback text for CV: {}", cvId);
        }
        inputs.add(new EmbeddingInput(
                EmbeddingType.SKILLS,
                buildEmbeddingText("cv_embed_skills", language, rawText, skills, parsedJson),
                0
        ));
        log.debug("Added SKILLS embedding for CV: {} ({} chars)", cvId, skills.length());

        // Type 4: EXPERIENCE - work experience
        String experience = extractExperience(parsedJson);
        if (experience.isBlank()) {
            experience = fallbackExperience(language);
            log.debug("EXPERIENCE is empty, using fallback text for CV: {}", cvId);
        }
        inputs.add(new EmbeddingInput(
                EmbeddingType.EXPERIENCE,
                buildEmbeddingText("cv_embed_experience", language, rawText, experience, parsedJson),
                0
        ));
        log.debug("Added EXPERIENCE embedding for CV: {} ({} chars)", cvId, experience.length());

        if (inputs.isEmpty()) {
            log.warn("No embedding inputs created for CV: {}", cvId);
            return;
        }

        log.info("Embedding {} inputs for CV: {} using provider: {}", 
                inputs.size(), cvId, providerRegistry.getEmbeddingProvider().providerName());

        // Call embedding provider
        EmbeddingProvider provider = providerRegistry.getEmbeddingProvider();
        List<String> texts = inputs.stream().map(EmbeddingInput::text).toList();
        long start = System.currentTimeMillis();

        try {
            // Use recommended dimensions based on provider
            Integer effectiveDimensions = aiProperties.getEmbedding().getRecommendedDimensions();
            
            log.debug("Calling embedding provider with {} texts, dimensions: {}", 
                    texts.size(), effectiveDimensions);

            EmbeddingResult result = provider.embed(EmbeddingRequest.builder()
                    .texts(texts)
                    .model(aiProperties.getEmbedding().getModel())
                    .dimensions(effectiveDimensions)
                    .timeoutMs(aiProperties.getEmbedding().getTimeoutMs())
                    .build());

            long latency = System.currentTimeMillis() - start;
            List<float[]> vectors = result.getVectors();
            String promptVersion = aiProperties.getPrompts().getActiveVersion();

            if (vectors.size() != inputs.size()) {
                throw new IllegalStateException(
                        "Embedding vector count mismatch. inputs=" + inputs.size() + ", vectors=" + vectors.size());
            }

            log.info("✓ Embedding generation succeeded for CV: {} - {} vectors in {}ms", 
                    cvId, vectors.size(), latency);

            // Store all embeddings
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
                log.debug("Stored {} embedding (chunk {}, {} tokens)", 
                        input.type(), input.chunkIndex(), embedding.getTokenCount());
            }

            log.info("✓ Stored {} embeddings for CV: {}", vectors.size(), cvId);

            // Log success metrics
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
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            log.error("✗ Embedding generation failed for CV: {} - {}", cvId, ex.getMessage(), ex);
            
            aiRunLoggingService.logFailure(
                    cvId,
                    requestId,
                    STEP_NAME,
                    provider.providerName(),
                    aiProperties.getEmbedding().getModel(),
                    null,
                    aiProperties.getPrompts().getActiveVersion(),
                    ex.getMessage(),
                    latency
            );
            throw ex;
        }
    }

    private String extractSummary(String parsedJson) {
        try {
            JsonNode root = objectMapper.readTree(parsedJson);
            String summary = root.path("summary").asText("");
            log.debug("Extracted summary: {} chars", summary.length());
            return summary;
        } catch (Exception e) {
            log.warn("Failed to extract summary: {}", e.getMessage());
            return "";
        }
    }

    private String extractSkills(String parsedJson) {
        try {
            JsonNode root = objectMapper.readTree(parsedJson);
            List<String> skills = new ArrayList<>();
            for (JsonNode node : root.path("extracted_skills")) {
                String skill = node.asText();
                if (!skill.isBlank()) {
                    skills.add(skill);
                }
            }
            String result = String.join(", ", skills);
            log.debug("Extracted {} skills: {} chars", skills.size(), result.length());
            return result;
        } catch (Exception e) {
            log.warn("Failed to extract skills: {}", e.getMessage());
            return "";
        }
    }

    private String extractExperience(String parsedJson) {
        try {
            JsonNode root = objectMapper.readTree(parsedJson);
            List<String> lines = new ArrayList<>();
            int count = 0;
            for (JsonNode exp : root.path("experiences")) {
                String company = exp.path("company").asText("");
                String title = exp.path("title").asText("");
                String description = exp.path("description").asText("");
                
                // Format: "Company | Title | Description"
                if (!company.isBlank() || !title.isBlank() || !description.isBlank()) {
                    String line = String.join(" | ", company, title, description);
                    lines.add(line);
                    count++;
                }
            }
            String result = String.join("\n", lines);
            log.debug("Extracted {} experiences: {} chars", count, result.length());
            return result;
        } catch (Exception e) {
            log.warn("Failed to extract experience: {}", e.getMessage());
            return "";
        }
    }

    private int approxTokenCount(String text) {
        return Math.max(1, text.length() / 4);
    }

    private String fallbackSummary(String language) {
        if ("vi".equalsIgnoreCase(language)) {
            return "Ung vien chua cung cap phan gioi thieu ban than ro rang trong CV.";
        }
        return "Candidate did not provide a clear professional summary in the CV.";
    }

    private String fallbackSkills(String language) {
        if ("vi".equalsIgnoreCase(language)) {
            return "Ung vien chua liet ke ky nang cu the trong CV.";
        }
        return "Candidate did not list specific skills in the CV.";
    }

    private String fallbackExperience(String language) {
        if ("vi".equalsIgnoreCase(language)) {
            return "Ung vien cap do entry-level, chua co kinh nghiem lam viec chinh thuc, dang tim co hoi thuc tap.";
        }
        return "Entry-level candidate, no formal work experience, looking for internship opportunities.";
    }

    private String buildEmbeddingText(String task, String language, String cvText, String sourceText, String parsedJson) {
        String version = aiProperties.getPrompts().getActiveVersion();
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("language", language == null ? "default" : language);
        vars.put("cv_text", cvText == null ? "" : cvText);
        vars.put("source_text", sourceText == null ? "" : sourceText);
        vars.put("parsed_json", parsedJson == null ? "" : parsedJson);

        try {
            return promptTemplateProvider.getPrompt(task, language, version, vars);
        } catch (Exception e) {
            log.warn("Embedding prompt not found for task={}, locale={}, fallback to source text", task, language);
            return sourceText;
        }
    }

    private record EmbeddingInput(EmbeddingType type, String text, int chunkIndex) {
    }
}
