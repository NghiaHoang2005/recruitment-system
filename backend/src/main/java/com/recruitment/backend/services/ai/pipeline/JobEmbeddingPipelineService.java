package com.recruitment.backend.services.ai.pipeline;

import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.JobEmbedding;
import com.recruitment.backend.domain.enums.JobEmbeddingType;
import com.recruitment.backend.domain.enums.RequirementSectionType;
import com.recruitment.backend.repositories.JobEmbeddingRepository;
import com.recruitment.backend.services.CacheManagementService;
import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.EmbeddingResult;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobEmbeddingPipelineService {

    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final EmbeddingBatchService embeddingBatchService;
    private final JobEmbeddingTextBuilder jobEmbeddingTextBuilder;
    private final AiProperties aiProperties;
    private final CacheManagementService cacheManagementService;

    @Transactional
    public void embedAndStore(Job job) {
        if (job == null || job.getId() == null) {
            log.debug("Skip job embedding because job is null or not persisted");
            return;
        }

        List<JobEmbeddingInput> inputs = buildJobEmbeddingInputs(job);
        if (inputs.isEmpty()) {
            log.debug("No embedding inputs created for job {}", job.getId());
            return;
        }

        List<String> texts = inputs.stream().map(JobEmbeddingInput::text).toList();
        EmbeddingResult result = embeddingBatchService.embedAll(texts);

        if (result.getVectors().size() != inputs.size()) {
            throw new IllegalStateException(
                    "Job embedding vector count mismatch. inputs=" + inputs.size() + ", vectors=" + result.getVectors().size());
        }

        String promptVersion = aiProperties.getPrompts().getActiveVersion();
        List<JobEmbedding> embeddings = new ArrayList<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            JobEmbeddingInput input = inputs.get(i);
            embeddings.add(JobEmbedding.builder()
                    .job(job)
                    .embeddingType(input.type())
                    .content(input.text())
                    .model(result.getModelName())
                    .promptVersion(promptVersion)
                    .dimensions(result.getDimensions())
                    .tokenCount(approxTokenCount(input.text()))
                    .vector(result.getVectors().get(i))
                    .build());
        }

        jobEmbeddingRepository.deleteByJob_Id(job.getId());
        jobEmbeddingRepository.saveAll(embeddings);

        log.info("Stored {} embeddings for job {}", embeddings.size(), job.getId());
        cacheManagementService.evictCacheForJob(job.getId());
    }

    private List<JobEmbeddingInput> buildJobEmbeddingInputs(Job job) {
        List<JobEmbeddingInput> inputs = new ArrayList<>();

        String fullText = jobEmbeddingTextBuilder.buildEmbeddingText(job);
        if (!fullText.isBlank()) {
            inputs.add(new JobEmbeddingInput(JobEmbeddingType.FULL_JOB, fullText));
        }

        String descriptionText = jobEmbeddingTextBuilder.buildDescriptionText(job);
        if (!descriptionText.isBlank()) {
            inputs.add(new JobEmbeddingInput(JobEmbeddingType.DESCRIPTION, descriptionText));
        }

        String skillsText = jobEmbeddingTextBuilder.buildSkillsText(job);
        if (!skillsText.isBlank()) {
            inputs.add(new JobEmbeddingInput(JobEmbeddingType.SKILLS, skillsText));
        }

        String requiredText = jobEmbeddingTextBuilder.buildRequirementText(
                job, RequirementSectionType.REQUIRED, "Required skills and experience");
        if (!requiredText.isBlank()) {
            inputs.add(new JobEmbeddingInput(JobEmbeddingType.REQUIRED_REQUIREMENTS, requiredText));
        }

        String preferredText = jobEmbeddingTextBuilder.buildRequirementText(
                job, RequirementSectionType.PREFERRED, "Preferred skills");
        if (!preferredText.isBlank()) {
            inputs.add(new JobEmbeddingInput(JobEmbeddingType.PREFERRED_REQUIREMENTS, preferredText));
        }

        return inputs;
    }

    private int approxTokenCount(String text) {
        return Math.max(1, text.length() / 4);
    }

    private record JobEmbeddingInput(JobEmbeddingType type, String text) {
    }
}
