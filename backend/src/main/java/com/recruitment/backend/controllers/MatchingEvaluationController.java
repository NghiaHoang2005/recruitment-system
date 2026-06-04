package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.Matching.*;
import com.recruitment.backend.domain.entities.Matching.MatchingEvaluationRun;
import com.recruitment.backend.domain.entities.Matching.MatchingRelevanceDataset;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.services.MatchingDatasetService;
import com.recruitment.backend.services.MatchingEvaluationService;
import com.recruitment.backend.services.MatchingMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingEvaluationController {

    private final MatchingDatasetService datasetService;
    private final MatchingEvaluationService evaluationService;
    private final MatchingMonitoringService monitoringService;

    // Relevance Dataset Management

    @PostMapping("/datasets")
    public ResponseEntity<MatchingDatasetResponse> createDataset(
            @RequestBody MatchingDatasetRequest request) {
        MatchingRelevanceDataset dataset = datasetService.createDataset(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDatasetResponse(dataset));
    }

    @GetMapping("/datasets/{id}")
    public ResponseEntity<MatchingDatasetResponse> getDataset(@PathVariable UUID id) {
        MatchingRelevanceDataset dataset = datasetService.getDataset(id)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_DATASET_NOT_FOUND));
        return ResponseEntity.ok(toDatasetResponse(dataset));
    }

    @GetMapping("/datasets")
    public ResponseEntity<List<MatchingDatasetResponse>> listDatasets(
            @RequestParam(required = false) UUID companyId) {
        List<MatchingRelevanceDataset> datasets = companyId != null
                ? datasetService.getDatasetsByCompany(companyId)
                : datasetService.getAllDatasets();
        return ResponseEntity.ok(datasets.stream()
                .map(this::toDatasetResponse)
                .collect(Collectors.toList()));
    }

    @PostMapping("/datasets/{datasetId}/pairs")
    public ResponseEntity<MatchingDatasetResponse> addPairToDataset(
            @PathVariable UUID datasetId,
            @RequestBody MatchingRelevancePairRequest request) {
        MatchingRelevanceDataset dataset = datasetService.addPair(datasetId, request);
        return ResponseEntity.ok(toDatasetResponse(dataset));
    }

    @GetMapping("/datasets/{datasetId}/pairs")
    public ResponseEntity<List<MatchingRelevancePairRequest>> getDatasetPairs(
            @PathVariable UUID datasetId) {
        List<MatchingRelevancePairRequest> pairs = datasetService.getPairsForDataset(datasetId);
        return ResponseEntity.ok(pairs);
    }

    @DeleteMapping("/datasets/{datasetId}")
    public ResponseEntity<Void> deleteDataset(@PathVariable UUID datasetId) {
        datasetService.deleteDataset(datasetId);
        return ResponseEntity.noContent().build();
    }

    // Evaluation Runs

    @PostMapping("/evaluate")
    public ResponseEntity<MatchingEvaluationResponse> runEvaluation(
            @RequestBody MatchingEvaluationRequest request) {
        MatchingEvaluationRun evaluationRun = evaluationService.runEvaluation(request);
        MatchingRelevanceDataset dataset = datasetService.getDataset(evaluationRun.getDatasetId()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toEvaluationResponse(evaluationRun, dataset));
    }

    @GetMapping("/evaluations/{evaluationId}")
    public ResponseEntity<MatchingEvaluationResponse> getEvaluation(
            @PathVariable UUID evaluationId) {
        MatchingEvaluationRun evaluation = evaluationService.getEvaluation(evaluationId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_EVALUATION_FAILED));
        MatchingRelevanceDataset dataset = datasetService.getDataset(evaluation.getDatasetId()).orElse(null);
        return ResponseEntity.ok(toEvaluationResponse(evaluation, dataset));
    }

    @GetMapping("/evaluations")
    public ResponseEntity<List<MatchingEvaluationResponse>> listEvaluations(
            @RequestParam(required = false) UUID datasetId,
            @RequestParam(required = false) UUID companyId) {
        List<MatchingEvaluationRun> evaluations;
        if (datasetId != null) {
            evaluations = evaluationService.getEvaluationsByDataset(datasetId);
        } else if (companyId != null) {
            evaluations = evaluationService.getEvaluationsByCompany(companyId);
        } else {
            evaluations = evaluationService.getAllEvaluations();
        }
        return ResponseEntity.ok(evaluations.stream()
                .map(evaluation -> {
                    MatchingRelevanceDataset dataset = datasetService.getDataset(evaluation.getDatasetId()).orElse(null);
                    return toEvaluationResponse(evaluation, dataset);
                })
                .collect(Collectors.toList()));
    }

    // Monitoring and Score Summary

    @GetMapping("/monitoring/summary")
    public ResponseEntity<MatchingScoreSummaryResponse> getScoreSummary(
            @RequestParam(required = false) com.recruitment.backend.domain.enums.MatchingRequestType requestType,
            @RequestParam(defaultValue = "1000") int limit) {
        com.recruitment.backend.domain.enums.MatchingRequestType type = requestType != null 
            ? requestType 
            : com.recruitment.backend.domain.enums.MatchingRequestType.MATCH;
        MatchingScoreSummaryResponse summary = monitoringService.getScoreSummary(type, limit);
        return ResponseEntity.ok(summary);
    }

    // Helper methods

    private MatchingDatasetResponse toDatasetResponse(MatchingRelevanceDataset dataset) {
        return MatchingDatasetResponse.builder()
                .id(dataset.getId())
                .companyId(dataset.getCompanyId())
                .name(dataset.getName())
                .description(dataset.getDescription())
                .direction(dataset.getDirection())
                .defaultTopK(dataset.getDefaultTopK())
                .pairCount(dataset.getPairCount())
                .version(dataset.getVersion())
                .createdAt(dataset.getCreatedAt())
                .updatedAt(dataset.getUpdatedAt())
                .build();
    }

    private MatchingEvaluationResponse toEvaluationResponse(MatchingEvaluationRun evaluation, MatchingRelevanceDataset dataset) {
        return MatchingEvaluationResponse.builder()
                .id(evaluation.getId())
                .datasetId(evaluation.getDatasetId())
                .datasetName(dataset != null ? dataset.getName() : null)
                .direction(dataset != null ? dataset.getDirection() : null)
                .weightProfileId(evaluation.getWeightProfileId())
                .weightVersion(evaluation.getWeightVersion())
                .companyId(evaluation.getCompanyId())
                .topK(evaluation.getTopK())
                .precisionAtK(evaluation.getPrecisionAtK())
                .recallAtK(evaluation.getRecallAtK())
                .f1AtK(evaluation.getF1AtK())
                .evaluatedPairs(evaluation.getEvaluatedPairs())
                .successfulPairs(evaluation.getSuccessfulPairs())
                .totalQueries(evaluation.getTotalQueries())
                .totalRelevant(evaluation.getTotalRelevant())
                .status(evaluation.getStatus())
                .errorMessage(evaluation.getErrorMessage())
                .version(evaluation.getVersion())
                .evaluatedAt(evaluation.getEvaluatedAt())
                .createdAt(evaluation.getCreatedAt())
                .updatedAt(evaluation.getUpdatedAt())
                .build();
    }
}
