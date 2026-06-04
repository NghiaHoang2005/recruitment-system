package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.entities.PipelineJob;
import com.recruitment.backend.services.PipelineJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/pipeline-jobs")
@RequiredArgsConstructor
public class PipelineJobController {

    private final PipelineJobService pipelineJobService;

    @PostMapping("/re-embed/cvs")
    public ApiResponse<PipelineJob> reEmbedAllCvs() {
        return ApiResponse.success(pipelineJobService.startReEmbedAllCvs());
    }

    @PostMapping("/re-embed/jobs")
    public ApiResponse<PipelineJob> reEmbedAllJobs() {
        return ApiResponse.success(pipelineJobService.startReEmbedAllJobs());
    }

    @PostMapping("/re-embed/cv/{cvId}")
    public ApiResponse<PipelineJob> reEmbedCv(@PathVariable UUID cvId) {
        return ApiResponse.success(pipelineJobService.startReEmbedSingleCv(cvId));
    }

    @PostMapping("/re-embed/job/{jobId}")
    public ApiResponse<PipelineJob> reEmbedJob(@PathVariable UUID jobId) {
        return ApiResponse.success(pipelineJobService.startReEmbedSingleJob(jobId));
    }

    @PostMapping("/rebuild-fts")
    public ApiResponse<PipelineJob> rebuildFtsIndex() {
        return ApiResponse.success(pipelineJobService.startRebuildFtsIndex());
    }

    @GetMapping
    public ApiResponse<List<PipelineJob>> getRecentJobs() {
        return ApiResponse.success(pipelineJobService.getRecentJobs());
    }

    @GetMapping("/{id}")
    public ApiResponse<PipelineJob> getJob(@PathVariable UUID id) {
        return ApiResponse.success(pipelineJobService.getJob(id));
    }

    @GetMapping("/active")
    public ApiResponse<List<PipelineJob>> getActiveJobs() {
        return ApiResponse.success(pipelineJobService.getActiveJobs());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<PipelineJob> cancelJob(@PathVariable UUID id) {
        return ApiResponse.success(pipelineJobService.cancelJob(id));
    }
}
