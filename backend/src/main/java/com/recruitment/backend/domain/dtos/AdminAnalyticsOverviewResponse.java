package com.recruitment.backend.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsOverviewResponse {
    private Overview overview;
    private Funnel funnel;
    private AiMetrics aiMetrics;
    private List<TimeSeriesPoint> applicationsByDay;
    private List<TimeSeriesPoint> jobsByDay;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {
        private long totalUsers;
        private long totalCompanies;
        private long totalJobs;
        private long totalApplications;
        private long jobsLast30Days;
        private long applicationsLast30Days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Funnel {
        private long applications;
        private long screening;
        private long interviews;
        private long offers;
        private long hires;
        private long rejected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiMetrics {
        private long scoredApplications;
        private Double averageApplicationAiScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesPoint {
        private LocalDate date;
        private long count;
    }
}
