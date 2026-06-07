package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RecruiterDashboardResponse {
    private Stats stats;
    private List<RecentApplicationItem> recentApplications;

    @Data
    @Builder
    public static class Stats {
        private long activeJobs;
        private long totalApplications;
        private long uniqueCandidates;
        private long applicationsLast7Days;
        private long interviewStageApplications;
        private double conversionRate;
        private Integer averageAiScore;
    }

    @Data
    @Builder
    public static class RecentApplicationItem {
        private UUID id;
        private UUID jobId;
        private String jobTitle;
        private UUID candidateId;
        private String candidateName;
        private String candidateEmail;
        private UUID cvId;
        private String cvName;
        private ApplicationStatus status;
        private Integer aiScore;
        private LocalDateTime appliedAt;
    }
}
