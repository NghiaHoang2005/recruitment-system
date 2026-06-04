package com.recruitment.backend.services;

import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.PipelineJob;
import com.recruitment.backend.domain.enums.PipelineJobStatus;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.repositories.PipelineJobRepository;
import com.recruitment.backend.services.ai.pipeline.CvEmbeddingPipelineService;
import com.recruitment.backend.services.ai.pipeline.JobEmbeddingPipelineService;
import com.recruitment.backend.services.ai.pipeline.TextNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineJobWorker {

    private final PipelineJobRepository pipelineJobRepository;
    private final CvRepository cvRepository;
    private final JobRepository jobRepository;
    private final CvEmbeddingPipelineService cvEmbeddingPipelineService;
    private final JobEmbeddingPipelineService jobEmbeddingPipelineService;
    private final TextNormalizationService textNormalizationService;

    @Async("taskExecutor")
    public void executeReEmbedAllCvs(UUID pipelineJobId) {
        PipelineJob job = startJob(pipelineJobId);
        if (job == null) return;

        try {
            List<Cv> cvs = cvRepository.findAll();
            job.setTotalItems(cvs.size());
            pipelineJobRepository.save(job);

            for (Cv cv : cvs) {
                if (isJobCancelled(pipelineJobId)) {
                    log.info("Pipeline job {} cancelled", pipelineJobId);
                    return;
                }
                try {
                    String rawText = cv.getRawText();
                    if (rawText == null || rawText.isBlank()) {
                        job.setFailedItems(job.getFailedItems() + 1);
                    } else {
                        String normalizedText = textNormalizationService.normalize(rawText);
                        List<String> chunks = textNormalizationService.chunkByApproxTokens(normalizedText);
                        String language = textNormalizationService.detectLanguage(rawText);
                        cvEmbeddingPipelineService.embedAndStore(
                                cv.getId(), rawText, normalizedText, cv.getParsedData(),
                                language, chunks);
                        job.setProcessedItems(job.getProcessedItems() + 1);
                    }
                } catch (Exception e) {
                    log.error("Failed to re-embed CV {}: {}", cv.getId(), e.getMessage());
                    job.setFailedItems(job.getFailedItems() + 1);
                }
                pipelineJobRepository.save(job);
            }

            completeJob(job);
        } catch (Exception e) {
            failJob(job, e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void executeReEmbedAllJobs(UUID pipelineJobId) {
        PipelineJob job = startJob(pipelineJobId);
        if (job == null) return;

        try {
            List<Job> jobs = jobRepository.findAll();
            job.setTotalItems(jobs.size());
            pipelineJobRepository.save(job);

            for (Job jobEntity : jobs) {
                if (isJobCancelled(pipelineJobId)) {
                    log.info("Pipeline job {} cancelled", pipelineJobId);
                    return;
                }
                try {
                    jobEmbeddingPipelineService.embedAndStore(jobEntity);
                    job.setProcessedItems(job.getProcessedItems() + 1);
                } catch (Exception e) {
                    log.error("Failed to re-embed Job {}: {}", jobEntity.getId(), e.getMessage());
                    job.setFailedItems(job.getFailedItems() + 1);
                }
                pipelineJobRepository.save(job);
            }

            completeJob(job);
        } catch (Exception e) {
            failJob(job, e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void executeReEmbedSingleCv(UUID pipelineJobId, UUID cvId) {
        PipelineJob job = startJob(pipelineJobId);
        if (job == null) return;

        try {
            job.setTotalItems(1);
            pipelineJobRepository.save(job);

            Cv cv = cvRepository.findById(cvId).orElseThrow();
            String rawText = cv.getRawText();
            if (rawText != null && !rawText.isBlank()) {
                String normalizedText = textNormalizationService.normalize(rawText);
                List<String> chunks = textNormalizationService.chunkByApproxTokens(normalizedText);
                String language = textNormalizationService.detectLanguage(rawText);
                cvEmbeddingPipelineService.embedAndStore(
                        cv.getId(), rawText, normalizedText, cv.getParsedData(),
                        language, chunks);
                job.setProcessedItems(1);
            }

            completeJob(job);
        } catch (Exception e) {
            failJob(job, e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void executeReEmbedSingleJob(UUID pipelineJobId, UUID jobId) {
        PipelineJob job = startJob(pipelineJobId);
        if (job == null) return;

        try {
            job.setTotalItems(1);
            pipelineJobRepository.save(job);

            Job jobEntity = jobRepository.findById(jobId).orElseThrow();
            jobEmbeddingPipelineService.embedAndStore(jobEntity);
            job.setProcessedItems(1);

            completeJob(job);
        } catch (Exception e) {
            failJob(job, e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void executeRebuildFtsIndex(UUID pipelineJobId) {
        PipelineJob job = startJob(pipelineJobId);
        if (job == null) return;

        try {
            long cvCount = cvRepository.count();
            long jobCount = jobRepository.count();
            job.setTotalItems((int) (cvCount + jobCount));
            pipelineJobRepository.save(job);

            List<Cv> cvs = cvRepository.findAll();
            for (Cv cv : cvs) {
                try {
                    cvRepository.save(cv);
                    job.setProcessedItems(job.getProcessedItems() + 1);
                } catch (Exception e) {
                    job.setFailedItems(job.getFailedItems() + 1);
                }
            }

            List<Job> jobs = jobRepository.findAll();
            for (Job jobEntity : jobs) {
                try {
                    jobRepository.save(jobEntity);
                    job.setProcessedItems(job.getProcessedItems() + 1);
                } catch (Exception e) {
                    job.setFailedItems(job.getFailedItems() + 1);
                }
            }
            pipelineJobRepository.save(job);

            completeJob(job);
        } catch (Exception e) {
            failJob(job, e.getMessage());
        }
    }

    private PipelineJob startJob(UUID pipelineJobId) {
        return pipelineJobRepository.findById(pipelineJobId).map(job -> {
            if (job.getStatus() == PipelineJobStatus.CANCELLED) return null;
            job.setStatus(PipelineJobStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            return pipelineJobRepository.save(job);
        }).orElse(null);
    }

    private void completeJob(PipelineJob job) {
        job.setStatus(PipelineJobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        pipelineJobRepository.save(job);
        log.info("Pipeline job {} completed: {}/{} items processed, {} failed",
                job.getId(), job.getProcessedItems(), job.getTotalItems(), job.getFailedItems());
    }

    private void failJob(PipelineJob job, String errorMessage) {
        job.setStatus(PipelineJobStatus.FAILED);
        job.setCompletedAt(LocalDateTime.now());
        job.setErrorMessage(errorMessage);
        pipelineJobRepository.save(job);
        log.error("Pipeline job {} failed: {}", job.getId(), errorMessage);
    }

    private boolean isJobCancelled(UUID pipelineJobId) {
        return pipelineJobRepository.findById(pipelineJobId)
                .map(j -> j.getStatus() == PipelineJobStatus.CANCELLED)
                .orElse(true);
    }
}
