package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.JobDTO;
import com.recruitment.backend.domain.dtos.JobRecommendationResponse;
import com.recruitment.backend.domain.dtos.Cv.CvRecommendationResponse;
import com.recruitment.backend.services.JobMatchService;
import com.recruitment.backend.services.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobMatchService jobMatchService;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getClaim("user_id"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobDTO>> createJob(@RequestBody JobDTO jobDTO, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(jobService.createJob(jobDTO, email)));
    }

    @PutMapping("/{id:[0-9a-fA-F-]{36}}")
    public ResponseEntity<ApiResponse<JobDTO>> updateJob(@PathVariable UUID id, @RequestBody JobDTO jobDTO, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(jobService.updateJob(id, jobDTO, email)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobDTO>>> getAllJobs(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(Object::toString)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(jobService.getJobsForUser(authentication.getName(), authorities)));
    }

    @GetMapping("/{id:[0-9a-fA-F-]{36}}")
    public ResponseEntity<ApiResponse<JobDTO>> getJobById(@PathVariable UUID id, Authentication authentication) {
        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(Object::toString)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(jobService.getJobById(id, authentication.getName(), authorities)));
    }

    @GetMapping("/{id:[0-9a-fA-F-]{36}}/match")
    public ResponseEntity<ApiResponse<JobMatchService.MatchScore>> getJobMatch(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID cvId
    ) {
        JobMatchService.MatchScore score = jobMatchService.matchJob(getCurrentUserId(), id, cvId);
        return ResponseEntity.ok(ApiResponse.success(score));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<JobRecommendationResponse>>> getJobRecommendations(
            @RequestParam(required = false) UUID cvId,
            @RequestParam(defaultValue = "10") int topK
    ) {
        List<JobMatchService.RecommendationScore> scores =
                jobMatchService.recommendJobs(getCurrentUserId(), cvId, topK);
        List<UUID> jobIds = scores.stream().map(JobMatchService.RecommendationScore::getJobId).toList();
        Map<UUID, JobDTO> jobMap = jobService.getJobsByIds(jobIds).stream()
                .collect(Collectors.toMap(JobDTO::getId, job -> job));

        List<JobRecommendationResponse> response = scores.stream()
                .map(score -> {
                    JobDTO job = jobMap.get(score.getJobId());
                    if (job == null) {
                        return null;
                    }
                    return JobRecommendationResponse.builder()
                            .job(job)
                            .matchScore(score.getFitScore())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id:[0-9a-fA-F-]{36}}/matches")
    public ResponseEntity<ApiResponse<List<CvRecommendationResponse>>> getCvMatches(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "10") int topK
    ) {
        List<CvRecommendationResponse> response = jobMatchService.recommendCvs(id, topK);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
