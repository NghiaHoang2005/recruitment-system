package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.Matching.MatchingEvaluationRequest;
import com.recruitment.backend.domain.entities.Matching.MatchingEvaluationRun;
import com.recruitment.backend.domain.entities.Matching.MatchingRelevancePair;
import com.recruitment.backend.domain.entities.Matching.MatchingWeightProfile;
import com.recruitment.backend.domain.enums.MatchingDatasetDirection;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.MatchingEvaluationRunRepository;
import com.recruitment.backend.repositories.MatchingRelevanceDatasetRepository;
import com.recruitment.backend.repositories.MatchingRelevancePairRepository;
import com.recruitment.backend.repositories.MatchingWeightProfileRepository;
import com.recruitment.backend.domain.dtos.Cv.CvRecommendationResponse;
import com.recruitment.backend.services.MatchingWeights;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingEvaluationService {

    private final MatchingEvaluationRunRepository evaluationRunRepository;
    private final MatchingRelevanceDatasetRepository datasetRepository;
    private final MatchingRelevancePairRepository pairRepository;
    private final MatchingWeightProfileRepository weightProfileRepository;
    private final JobMatchService jobMatchService;
    private final MatchingWeightService matchingWeightService;

    @Transactional
    public MatchingEvaluationRun runEvaluation(MatchingEvaluationRequest request) {
        var dataset = datasetRepository.findById(request.getDatasetId())
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_DATASET_NOT_FOUND));

        Integer topK = request.getTopK();
        if (topK == null) {
            topK = dataset.getDefaultTopK();
        }
        if (topK == null) {
            topK = 10;
        }
        if (topK <= 0) {
            topK = 10;
        }

        UUID weightProfileId = request.getWeightProfileId();
        MatchingWeightProfile weightProfile = null;
        if (weightProfileId != null) {
            weightProfile = weightProfileRepository.findById(weightProfileId)
                    .orElseThrow(() -> new AppException(ErrorCode.MATCHING_WEIGHT_PROFILE_NOT_FOUND));
        }

        List<MatchingRelevancePair> pairs = pairRepository.findByDataset_Id(dataset.getId());
        if (pairs.isEmpty()) {
            throw new AppException(ErrorCode.MATCHING_EVALUATION_FAILED);
        }

        MatchingWeights weightsOverride = weightProfile != null ? MatchingWeights.fromProfile(weightProfile) : null;
        EvaluationMetrics metrics = computeMetrics(
                dataset.getDirection(),
                pairs,
                topK,
                weightsOverride
        );

        MatchingWeights resolvedWeights = weightsOverride != null
                ? weightsOverride
                : matchingWeightService.resolveWeightsForCompany(dataset.getCompanyId());
        String weightVersion = resolvedWeights != null ? resolvedWeights.version() : null;
        String status = metrics.totalQueries == 0 ? "FAILED" : (metrics.failedQueries > 0 ? "PARTIAL" : "COMPLETED");
        String errorMessage = metrics.totalQueries == 0
                ? "No successful queries were evaluated"
                : (metrics.failedQueries > 0 ? "Failed queries: " + metrics.failedQueries : null);

        MatchingEvaluationRun run = evaluationRunRepository.save(MatchingEvaluationRun.builder()
                .datasetId(dataset.getId())
                .companyId(dataset.getCompanyId())
                .weightProfileId(weightProfileId)
                .weightVersion(weightVersion)
                .topK(topK)
                .precisionAtK(metrics.precisionAtK)
                .recallAtK(metrics.recallAtK)
                .f1AtK(metrics.f1AtK)
                .totalQueries(metrics.totalQueries)
                .totalRelevant(metrics.totalRelevant)
                .evaluatedPairs(metrics.evaluatedPairs)
                .successfulPairs(metrics.successfulPairs)
                .status(status)
                .errorMessage(errorMessage)
                .version("1.0")
                .build());

        return run;
    }

    public Optional<MatchingEvaluationRun> getEvaluation(UUID evaluationId) {
        return evaluationRunRepository.findById(evaluationId);
    }

    public List<MatchingEvaluationRun> getEvaluationsByDataset(UUID datasetId) {
        return evaluationRunRepository.findByDatasetIdOrderByCreatedAtDesc(datasetId);
    }

    public List<MatchingEvaluationRun> getEvaluationsByCompany(UUID companyId) {
        return evaluationRunRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    public List<MatchingEvaluationRun> getAllEvaluations() {
        return evaluationRunRepository.findAllByOrderByCreatedAtDesc();
    }

    private EvaluationMetrics computeMetrics(
            MatchingDatasetDirection direction,
            List<MatchingRelevancePair> pairs,
            int topK,
            MatchingWeights weightsOverride
    ) {
        int totalQueries = 0;
        int totalRelevant = 0;
        int retrievedRelevant = 0;
        int evaluatedPairs = 0;
        int successfulPairs = 0;
        int failedQueries = 0;

        Map<UUID, List<MatchingRelevancePair>> grouped = pairs.stream()
                .filter(pair -> direction == MatchingDatasetDirection.CV_TO_JOB ? pair.getCvId() != null : pair.getJobId() != null)
                .collect(Collectors.groupingBy(pair -> direction == MatchingDatasetDirection.CV_TO_JOB
                        ? pair.getCvId()
                        : pair.getJobId()));

        for (Map.Entry<UUID, List<MatchingRelevancePair>> entry : grouped.entrySet()) {
            UUID queryId = entry.getKey();
            List<MatchingRelevancePair> groupPairs = entry.getValue();
            List<?> results;

            try {
                if (direction == MatchingDatasetDirection.CV_TO_JOB) {
                    results = jobMatchService.recommendJobsForEvaluation(queryId, topK, weightsOverride);
                } else {
                    results = jobMatchService.recommendCvsForEvaluation(queryId, topK, weightsOverride);
                }
            } catch (AppException ex) {
                failedQueries++;
                continue;
            }

            totalQueries++;
            evaluatedPairs += groupPairs.size();

            int groupRelevant = groupPairs.stream()
                    .map(pair -> pair.getRelevance() != null ? pair.getRelevance() : 1)
                    .reduce(0, Integer::sum);
            totalRelevant += groupRelevant;

            if (results == null || results.isEmpty()) {
                continue;
            }

            Set<UUID> resultIds = new HashSet<>();
            if (direction == MatchingDatasetDirection.CV_TO_JOB) {
                results.stream()
                        .filter(r -> r instanceof JobMatchService.RecommendationScore)
                        .map(r -> ((JobMatchService.RecommendationScore) r).getJobId())
                        .forEach(resultIds::add);
            } else {
                results.stream()
                        .filter(r -> r instanceof CvRecommendationResponse)
                        .map(r -> ((CvRecommendationResponse) r).getCv().getId())
                        .forEach(resultIds::add);
            }

            for (MatchingRelevancePair pair : groupPairs) {
                UUID expectedId = direction == MatchingDatasetDirection.CV_TO_JOB
                        ? pair.getJobId()
                        : pair.getCvId();
                if (expectedId != null && resultIds.contains(expectedId)) {
                    retrievedRelevant += pair.getRelevance() != null ? pair.getRelevance() : 1;
                    successfulPairs += 1;
                }
            }
        }

        if (totalQueries == 0) {
            return new EvaluationMetrics(0.0, 0.0, 0.0, 0, 0, 0, 0, 0, failedQueries);
        }

        double precision = (double) retrievedRelevant / (totalQueries * topK);
        double recall = totalRelevant > 0 ? (double) retrievedRelevant / totalRelevant : 0.0;
        double f1 = computeF1(precision, recall);

        return new EvaluationMetrics(
                precision,
                recall,
                f1,
                totalQueries,
                totalRelevant,
                retrievedRelevant,
                evaluatedPairs,
                successfulPairs,
                failedQueries
        );
    }

    private double computeF1(double precision, double recall) {
        if (precision + recall == 0) {
            return 0.0;
        }
        return 2.0 * (precision * recall) / (precision + recall);
    }

    private record EvaluationMetrics(
            double precisionAtK,
            double recallAtK,
            double f1AtK,
            int totalQueries,
            int totalRelevant,
            int retrievedRelevant,
            int evaluatedPairs,
            int successfulPairs,
            int failedQueries
    ) {
    }
}

