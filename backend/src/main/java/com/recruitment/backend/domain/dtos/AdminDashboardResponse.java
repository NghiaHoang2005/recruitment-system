package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.CompanyStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private Metrics metrics;
    private List<CompanyQueueItem> pendingCompanies;
    private List<JobQueueItem> pendingJobs;
    private List<JobQueueItem> recentJobs;
    private List<ApplicationActivityItem> recentApplications;
    private List<ActivityItem> recentActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {
        private long totalUsers;
        private long totalCandidates;
        private long totalRecruiters;
        private long totalAdmins;
        private long totalCompanies;
        private long pendingCompanies;
        private long activeCompanies;
        private long totalJobs;
        private long publishedJobs;
        private long pendingJobs;
        private long flaggedJobs;
        private long rejectedJobs;
        private long totalApplications;
        private long applicationsLast7Days;
        private long applicationsLast30Days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyQueueItem {
        private UUID id;
        private String name;
        private String email;
        private String website;
        private String industry;
        private CompanyStatus status;
        private String ownerEmail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobQueueItem {
        private UUID id;
        private String title;
        private UUID companyId;
        private String companyName;
        private JobStatus status;
        private String location;
        private LocalDateTime createdAt;
        private LocalDateTime publishedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationActivityItem {
        private UUID id;
        private UUID jobId;
        private String jobTitle;
        private String companyName;
        private String candidateName;
        private String candidateEmail;
        private Integer aiScore;
        private LocalDateTime appliedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String type;
        private String title;
        private String description;
        private LocalDateTime occurredAt;
    }
}
