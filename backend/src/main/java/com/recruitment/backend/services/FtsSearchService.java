package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.Cv.CvFtsSearchResponse;
import com.recruitment.backend.domain.dtos.Cv.CvItemResponse;
import com.recruitment.backend.domain.dtos.JobDTO;
import com.recruitment.backend.domain.dtos.JobFtsSearchResponse;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.mappers.JobMapper;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.repositories.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FtsSearchService {

    private static final int MAX_LIMIT = 50;

    private final JobRepository jobRepository;
    private final CvRepository cvRepository;
    private final JobMapper jobMapper;

    public List<JobFtsSearchResponse> searchJobs(String query, JobStatus status, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int safeLimit = clampLimit(limit);
        String statusValue = status == null ? JobStatus.PUBLISHED.name() : status.name();

        List<JobRepository.JobFtsView> rows = jobRepository.searchJobsByFts(query, statusValue, safeLimit);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<UUID> jobIds = rows.stream()
                .map(row -> UUID.fromString(row.getJobId()))
                .toList();
        Map<UUID, JobDTO> jobMap = jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, jobMapper::toDto));

        return rows.stream()
                .map(row -> {
                    JobDTO job = jobMap.get(UUID.fromString(row.getJobId()));
                    if (job == null) {
                        return null;
                    }
                    return JobFtsSearchResponse.builder()
                            .job(job)
                            .rank(row.getRank())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public List<CvFtsSearchResponse> searchCvs(String query, UUID candidateId, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int safeLimit = clampLimit(limit);
        List<CvRepository.CvFtsView> rows = cvRepository.searchCvsByFts(query, candidateId, safeLimit);
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
                    return CvFtsSearchResponse.builder()
                            .cv(item)
                            .rank(row.getRank())
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
