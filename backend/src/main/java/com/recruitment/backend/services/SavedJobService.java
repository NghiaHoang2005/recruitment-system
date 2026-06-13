package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.SavedJobResponse;
import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.SavedJob;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.mappers.JobMapper;
import com.recruitment.backend.repositories.CandidateRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.repositories.SavedJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedJobService {
    private final SavedJobRepository savedJobRepository;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    @Transactional(readOnly = true)
    public List<SavedJobResponse> getSavedJobs(UUID candidateId) {
        return savedJobRepository.findByCandidate_UserIdOrderBySavedAtDesc(candidateId)
                .stream()
                .filter(savedJob -> savedJob.getJob().getStatus() == JobStatus.PUBLISHED)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SavedJobResponse saveJob(UUID candidateId, UUID jobId) {
        return savedJobRepository.findByCandidate_UserIdAndJob_Id(candidateId, jobId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    Candidate candidate = candidateRepository.findById(candidateId)
                            .orElseThrow(() -> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));
                    Job job = jobRepository.findById(jobId)
                            .filter(item -> item.getStatus() == JobStatus.PUBLISHED)
                            .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
                    SavedJob savedJob = savedJobRepository.save(SavedJob.builder()
                            .candidate(candidate)
                            .job(job)
                            .build());
                    return toResponse(savedJob);
                });
    }

    @Transactional
    public void removeSavedJob(UUID candidateId, UUID jobId) {
        savedJobRepository.deleteByCandidate_UserIdAndJob_Id(candidateId, jobId);
    }

    @Transactional(readOnly = true)
    public boolean isSaved(UUID candidateId, UUID jobId) {
        return savedJobRepository.existsByCandidate_UserIdAndJob_Id(candidateId, jobId);
    }

    @Transactional(readOnly = true)
    public long countSavedJobs(UUID candidateId) {
        return savedJobRepository.countByCandidate_UserIdAndJob_Status(candidateId, JobStatus.PUBLISHED);
    }

    private SavedJobResponse toResponse(SavedJob savedJob) {
        return SavedJobResponse.builder()
                .job(jobMapper.toDto(savedJob.getJob()))
                .savedAt(savedJob.getSavedAt())
                .build();
    }
}
