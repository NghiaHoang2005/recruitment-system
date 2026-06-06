package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.AdminAnalyticsOverviewResponse;
import com.recruitment.backend.domain.entities.Application;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.enums.ApplicationStatus;
import com.recruitment.backend.repositories.ApplicationRepository;
import com.recruitment.backend.repositories.CompanyRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminAnalyticsOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now();
        LocalDate rangeStart = fromDate == null ? today.minusDays(29) : fromDate;
        LocalDate rangeEnd = toDate == null ? today : toDate;
        if (rangeEnd.isBefore(rangeStart)) {
            rangeEnd = rangeStart;
        }
        LocalDateTime fromDateTime = rangeStart.atStartOfDay();
        LocalDateTime toDateTime = rangeEnd.atTime(LocalTime.MAX);
        List<Application> recentApplications = applicationRepository.findByAppliedAtBetween(fromDateTime, toDateTime);
        List<Job> recentJobs = jobRepository.findByCreatedAtBetween(fromDateTime, toDateTime);

        return AdminAnalyticsOverviewResponse.builder()
                .overview(AdminAnalyticsOverviewResponse.Overview.builder()
                        .totalUsers(userRepository.count())
                        .totalCompanies(companyRepository.count())
                        .totalJobs(jobRepository.count())
                        .totalApplications(applicationRepository.count())
                        .jobsLast30Days(jobRepository.countByCreatedAtBetween(fromDateTime, toDateTime))
                        .applicationsLast30Days(applicationRepository.countByAppliedAtBetween(fromDateTime, toDateTime))
                        .build())
                .funnel(AdminAnalyticsOverviewResponse.Funnel.builder()
                        .applications(applicationRepository.count())
                        .screening(applicationRepository.countByStatus(ApplicationStatus.SCREENING))
                        .interviews(applicationRepository.countByStatus(ApplicationStatus.INTERVIEW))
                        .offers(applicationRepository.countByStatus(ApplicationStatus.OFFERED))
                        .hires(applicationRepository.countByStatus(ApplicationStatus.HIRED))
                        .rejected(applicationRepository.countByStatus(ApplicationStatus.REJECTED))
                        .build())
                .aiMetrics(buildAiMetrics())
                .applicationsByDay(buildApplicationSeries(recentApplications, rangeStart, rangeEnd))
                .jobsByDay(buildJobSeries(recentJobs, rangeStart, rangeEnd))
                .build();
    }

    private AdminAnalyticsOverviewResponse.AiMetrics buildAiMetrics() {
        List<Application> scoredApplications = applicationRepository.findAll()
                .stream()
                .filter(application -> application.getAiScore() != null)
                .toList();
        Double averageScore = scoredApplications.isEmpty()
                ? null
                : scoredApplications.stream().mapToInt(Application::getAiScore).average().orElse(0);

        return AdminAnalyticsOverviewResponse.AiMetrics.builder()
                .scoredApplications(scoredApplications.size())
                .averageApplicationAiScore(averageScore)
                .build();
    }

    private List<AdminAnalyticsOverviewResponse.TimeSeriesPoint> buildApplicationSeries(
            List<Application> applications,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, Long> counts = emptyDateRange(startDate, endDate);
        applications.forEach(application -> {
            if (application.getAppliedAt() != null) {
                LocalDate date = application.getAppliedAt().toLocalDate();
                counts.computeIfPresent(date, (key, value) -> value + 1);
            }
        });
        return toSeries(counts);
    }

    private List<AdminAnalyticsOverviewResponse.TimeSeriesPoint> buildJobSeries(
            List<Job> jobs,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, Long> counts = emptyDateRange(startDate, endDate);
        jobs.forEach(job -> {
            if (job.getCreatedAt() != null) {
                LocalDate date = job.getCreatedAt().toLocalDate();
                counts.computeIfPresent(date, (key, value) -> value + 1);
            }
        });
        return toSeries(counts);
    }

    private Map<LocalDate, Long> emptyDateRange(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            counts.put(current, 0L);
            current = current.plusDays(1);
        }
        return counts;
    }

    private List<AdminAnalyticsOverviewResponse.TimeSeriesPoint> toSeries(Map<LocalDate, Long> counts) {
        return counts.entrySet()
                .stream()
                .map(entry -> AdminAnalyticsOverviewResponse.TimeSeriesPoint.builder()
                        .date(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }
}
