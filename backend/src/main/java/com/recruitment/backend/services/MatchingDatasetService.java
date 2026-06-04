package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.Matching.MatchingDatasetRequest;
import com.recruitment.backend.domain.dtos.Matching.MatchingRelevancePairRequest;
import com.recruitment.backend.domain.entities.Matching.MatchingRelevanceDataset;
import com.recruitment.backend.domain.entities.Matching.MatchingRelevancePair;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.MatchingRelevanceDatasetRepository;
import com.recruitment.backend.repositories.MatchingRelevancePairRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingDatasetService {

    private final MatchingRelevanceDatasetRepository datasetRepository;
    private final MatchingRelevancePairRepository pairRepository;

    @Transactional
    public MatchingRelevanceDataset createDataset(MatchingDatasetRequest request) {
        MatchingRelevanceDataset dataset = datasetRepository.save(MatchingRelevanceDataset.builder()
                .name(request.getName())
                .description(request.getDescription())
                .direction(request.getDirection())
                .companyId(request.getCompanyId())
                .defaultTopK(request.getDefaultTopK())
                .pairCount(0)
                .version("1.0")
                .build());

        if (request.getPairs() != null && !request.getPairs().isEmpty()) {
            for (MatchingRelevancePairRequest pairRequest : request.getPairs()) {
                pairRepository.save(MatchingRelevancePair.builder()
                        .dataset(dataset)
                        .cvId(pairRequest.getCvId())
                        .jobId(pairRequest.getJobId())
                        .relevance(pairRequest.getRelevance() != null ? pairRequest.getRelevance() : 1)
                        .notes(pairRequest.getNotes())
                        .build());
            }
            dataset.setPairCount(request.getPairs().size());
            dataset = datasetRepository.save(dataset);
        }

        return dataset;
    }

    public Optional<MatchingRelevanceDataset> getDataset(UUID datasetId) {
        return datasetRepository.findById(datasetId);
    }

    public List<MatchingRelevanceDataset> getAllDatasets() {
        return datasetRepository.findAll();
    }

    public List<MatchingRelevanceDataset> getDatasetsByCompany(UUID companyId) {
        return datasetRepository.findByCompanyId(companyId);
    }

    public List<MatchingRelevancePairRequest> getPairsForDataset(UUID datasetId) {
        return pairRepository.findByDataset_Id(datasetId).stream()
                .map(pair -> MatchingRelevancePairRequest.builder()
                        .cvId(pair.getCvId())
                        .jobId(pair.getJobId())
                        .relevance(pair.getRelevance())
                        .notes(pair.getNotes())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public MatchingRelevanceDataset addPair(UUID datasetId, MatchingRelevancePairRequest request) {
        MatchingRelevanceDataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_DATASET_NOT_FOUND));

        pairRepository.save(MatchingRelevancePair.builder()
                .dataset(dataset)
                .cvId(request.getCvId())
                .jobId(request.getJobId())
                .relevance(request.getRelevance() != null ? request.getRelevance() : 1)
                .notes(request.getNotes())
                .build());

        dataset.setPairCount(dataset.getPairCount() + 1);
        dataset.setVersion(bumpVersion(dataset.getVersion()));
        return datasetRepository.save(dataset);
    }

    @Transactional
    public MatchingRelevanceDataset addPairs(UUID datasetId, List<MatchingRelevancePairRequest> requests) {
        MatchingRelevanceDataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_DATASET_NOT_FOUND));

        for (MatchingRelevancePairRequest request : requests) {
            pairRepository.save(MatchingRelevancePair.builder()
                    .dataset(dataset)
                    .cvId(request.getCvId())
                    .jobId(request.getJobId())
                    .relevance(request.getRelevance() != null ? request.getRelevance() : 1)
                    .notes(request.getNotes())
                    .build());
        }

        dataset.setPairCount(dataset.getPairCount() + requests.size());
        dataset.setVersion(bumpVersion(dataset.getVersion()));
        return datasetRepository.save(dataset);
    }

    @Transactional
    public void deleteDataset(UUID datasetId) {
        MatchingRelevanceDataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_DATASET_NOT_FOUND));
        
        pairRepository.deleteByDataset_Id(datasetId);
        datasetRepository.delete(dataset);
    }

    private String bumpVersion(String current) {
        if (current == null || current.isBlank()) {
            return "1.0";
        }
        String trimmed = current.trim();
        String[] parts = trimmed.split("\\.");
        if (parts.length == 0) {
            return "1.0";
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return major + "." + (minor + 1);
        } catch (NumberFormatException ex) {
            return trimmed + ".1";
        }
    }
}

