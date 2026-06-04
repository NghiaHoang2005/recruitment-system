package com.recruitment.backend.services;

import com.recruitment.backend.domain.entities.PipelineJob;
import com.recruitment.backend.domain.enums.PipelineJobStatus;
import com.recruitment.backend.domain.enums.PipelineJobType;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.PipelineJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineJobService {

    private final PipelineJobRepository pipelineJobRepository;
    private final PipelineJobWorker pipelineJobWorker;

    @Transactional
    public PipelineJob startReEmbedAllCvs() {
        checkNoRunningJob(PipelineJobType.RE_EMBED_ALL_CVS);
        PipelineJob job = createJob(PipelineJobType.RE_EMBED_ALL_CVS, null);
        pipelineJobWorker.executeReEmbedAllCvs(job.getId());
        return job;
    }

    @Transactional
    public PipelineJob startReEmbedAllJobs() {
        checkNoRunningJob(PipelineJobType.RE_EMBED_ALL_JOBS);
        PipelineJob job = createJob(PipelineJobType.RE_EMBED_ALL_JOBS, null);
        pipelineJobWorker.executeReEmbedAllJobs(job.getId());
        return job;
    }

    @Transactional
    public PipelineJob startReEmbedSingleCv(UUID cvId) {
        PipelineJob job = createJob(PipelineJobType.RE_EMBED_SINGLE_CV, cvId);
        pipelineJobWorker.executeReEmbedSingleCv(job.getId(), cvId);
        return job;
    }

    @Transactional
    public PipelineJob startReEmbedSingleJob(UUID jobId) {
        PipelineJob job = createJob(PipelineJobType.RE_EMBED_SINGLE_JOB, jobId);
        pipelineJobWorker.executeReEmbedSingleJob(job.getId(), jobId);
        return job;
    }

    @Transactional
    public PipelineJob startRebuildFtsIndex() {
        checkNoRunningJob(PipelineJobType.REBUILD_FTS_INDEX);
        PipelineJob job = createJob(PipelineJobType.REBUILD_FTS_INDEX, null);
        pipelineJobWorker.executeRebuildFtsIndex(job.getId());
        return job;
    }

    public PipelineJob getJob(UUID jobId) {
        return pipelineJobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.PIPELINE_JOB_NOT_FOUND));
    }

    public List<PipelineJob> getActiveJobs() {
        return pipelineJobRepository.findByStatusIn(
                List.of(PipelineJobStatus.QUEUED, PipelineJobStatus.RUNNING));
    }

    public List<PipelineJob> getRecentJobs() {
        return pipelineJobRepository.findTop20ByOrderByCreatedAtDesc();
    }

    @Transactional
    public PipelineJob cancelJob(UUID jobId) {
        PipelineJob job = getJob(jobId);
        if (job.getStatus() != PipelineJobStatus.QUEUED) {
            throw new AppException(ErrorCode.PIPELINE_JOB_CANNOT_CANCEL);
        }
        job.setStatus(PipelineJobStatus.CANCELLED);
        return pipelineJobRepository.save(job);
    }

    private PipelineJob createJob(PipelineJobType type, UUID targetId) {
        PipelineJob job = PipelineJob.builder()
                .jobType(type)
                .status(PipelineJobStatus.QUEUED)
                .targetId(targetId)
                .totalItems(0)
                .processedItems(0)
                .failedItems(0)
                .build();
        return pipelineJobRepository.save(job);
    }

    private void checkNoRunningJob(PipelineJobType type) {
        boolean exists = pipelineJobRepository.existsByJobTypeAndStatusIn(
                type, List.of(PipelineJobStatus.QUEUED, PipelineJobStatus.RUNNING));
        if (exists) {
            throw new AppException(ErrorCode.PIPELINE_JOB_ALREADY_RUNNING);
        }
    }
}
