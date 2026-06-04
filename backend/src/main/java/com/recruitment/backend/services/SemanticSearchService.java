package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.Cv.CvItemResponse;
import com.recruitment.backend.domain.dtos.Cv.CvSemanticSearchResponse;
import com.recruitment.backend.domain.dtos.JobDTO;
import com.recruitment.backend.domain.dtos.JobSemanticSearchResponse;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.EmbeddingType;
import com.recruitment.backend.domain.enums.JobEmbeddingType;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.mappers.JobMapper;
import com.recruitment.backend.repositories.CvEmbeddingRepository;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.repositories.JobEmbeddingRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.services.ai.model.EmbeddingResult;
import com.recruitment.backend.services.ai.pipeline.EmbeddingBatchService;
import com.recruitment.backend.utils.VectorSearchUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private static final int MAX_LIMIT = 50;

    private final EmbeddingBatchService embeddingBatchService;
    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final CvEmbeddingRepository cvEmbeddingRepository;
    private final JobRepository jobRepository;
    private final CvRepository cvRepository;
    private final JobMapper jobMapper;

    public List<JobSemanticSearchResponse> searchJobs(String query, JobEmbeddingType type, JobStatus status, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        int safeLimit = clampLimit(limit);
        JobEmbeddingType safeType = type == null ? JobEmbeddingType.FULL_JOB : type;
        String statusValue = status == null ? null : status.name();

        EmbeddingResult embedding = embeddingBatchService.embedAll(List.of(query));
        if (embedding.getVectors() == null || embedding.getVectors().isEmpty()) {
            return List.of();
        }

        String model = embedding.getModelName();
        Integer dimensions = embedding.getDimensions();
        if (model == null || dimensions == null) {
            return List.of();
        }

        String vectorLiteral = VectorSearchUtil.toVectorLiteral(embedding.getVectors().get(0));
        List<JobEmbeddingRepository.JobEmbeddingScoreView> rows =
                jobEmbeddingRepository.findTopJobScoresByTypeModelDimensionsAndStatus(
                        vectorLiteral, safeType.name(), model, dimensions, statusValue, safeLimit);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<UUID> jobIds = rows.stream()
                .map(row -> UUID.fromString(row.getJobId()))
                .toList();
        Map<UUID, JobDTO> jobMap = jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(job -> job.getId(), jobMapper::toDto));

        return rows.stream()
                .map(row -> {
                    JobDTO job = jobMap.get(UUID.fromString(row.getJobId()));
                    if (job == null) {
                        return null;
                    }
                    Double distance = row.getDistance();
                    return JobSemanticSearchResponse.builder()
                            .job(job)
                            .distance(distance)
                            .similarity(VectorSearchUtil.distanceToSimilarity(distance))
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public List<CvSemanticSearchResponse> searchCvs(String query, EmbeddingType type, UUID candidateId, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        int safeLimit = clampLimit(limit);
        EmbeddingType safeType = type == null ? EmbeddingType.SUMMARY : type;

        EmbeddingResult embedding = embeddingBatchService.embedAll(List.of(query));
        if (embedding.getVectors() == null || embedding.getVectors().isEmpty()) {
            return List.of();
        }

        String model = embedding.getModelName();
        Integer dimensions = embedding.getDimensions();
        if (model == null || dimensions == null) {
            return List.of();
        }

        String vectorLiteral = VectorSearchUtil.toVectorLiteral(embedding.getVectors().get(0));
        List<CvEmbeddingRepository.CvEmbeddingScoreView> rows =
                cvEmbeddingRepository.findTopCvScoresByTypeModelDimensionsAndCandidate(
                        vectorLiteral, safeType.name(), model, dimensions, candidateId, safeLimit);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<UUID> cvIds = rows.stream()
                .map(row -> UUID.fromString(row.getCvId()))
                .toList();
        Map<UUID, Cv> cvMap = cvRepository.findAllById(cvIds).stream()
                .collect(Collectors.toMap(Cv::getId, cv -> cv));

        return rows.stream()
                .map(row -> {
                    Cv cv = cvMap.get(UUID.fromString(row.getCvId()));
                    if (cv == null) {
                        return null;
                    }
                    CvItemResponse item = CvItemResponse.builder()
                            .id(cv.getId())
                            .cvName(cv.getCvName())
                            .uploadedAt(cv.getUploadedAt())
                            .isDefault(Boolean.TRUE.equals(cv.getIsDefault()))
                            .aiStatus(cv.getAiStatus())
                            .build();
                    Double distance = row.getDistance();
                    return CvSemanticSearchResponse.builder()
                            .cv(item)
                            .distance(distance)
                            .similarity(VectorSearchUtil.distanceToSimilarity(distance))
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
