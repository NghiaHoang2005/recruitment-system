package com.recruitment.backend.services;

import com.recruitment.backend.domain.entities.PipelineJob;
import com.recruitment.backend.domain.enums.PipelineJobStatus;
import com.recruitment.backend.repositories.PipelineJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineHealthScheduler {

    private final PipelineJobRepository pipelineJobRepository;

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void checkStalledPipelines() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<PipelineJob> stalledJobs = pipelineJobRepository.findStalledJobs(threshold);

        if (!stalledJobs.isEmpty()) {
            log.warn("Detected {} stalled pipeline jobs", stalledJobs.size());
            for (PipelineJob job : stalledJobs) {
                job.setStatus(PipelineJobStatus.FAILED);
                job.setCompletedAt(LocalDateTime.now());
                job.setErrorMessage("Job stalled - automatically marked as failed after 30 minutes");
                pipelineJobRepository.save(job);
                log.warn("Marked stalled pipeline job {} ({}) as FAILED", job.getId(), job.getJobType());
            }
        }
    }
}
