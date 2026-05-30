package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.AdminDashboardResponse;
import com.recruitment.backend.domain.entities.Application;
import com.recruitment.backend.domain.entities.Company;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.enums.CompanyStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.repositories.ApplicationRepository;
import com.recruitment.backend.repositories.CompanyRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        List<Company> pendingCompanies = companyRepository.findTop5ByStatusOrderByNameAsc(CompanyStatus.PENDING);
        List<Job> pendingJobs = jobRepository.findTop5ByStatusOrderByCreatedAtDesc(JobStatus.PENDING);
        List<Job> recentJobs = jobRepository.findTop5ByOrderByCreatedAtDesc();
        List<Application> recentApplications = applicationRepository.findTop5ByOrderByAppliedAtDesc();

        return AdminDashboardResponse.builder()
                .metrics(buildMetrics())
                .pendingCompanies(pendingCompanies.stream().map(this::toCompanyQueueItem).toList())
                .pendingJobs(pendingJobs.stream().map(this::toJobQueueItem).toList())
                .recentJobs(recentJobs.stream().map(this::toJobQueueItem).toList())
                .recentApplications(recentApplications.stream().map(this::toApplicationActivityItem).toList())
                .recentActivity(buildRecentActivity(pendingCompanies, pendingJobs, recentApplications))
                .build();
    }

    private AdminDashboardResponse.Metrics buildMetrics() {
        LocalDateTime now = LocalDateTime.now();

        return AdminDashboardResponse.Metrics.builder()
                .totalUsers(userRepository.count())
                .totalCandidates(userRepository.countByRole_Name("CANDIDATE"))
                .totalRecruiters(userRepository.countByRole_Name("RECRUITER"))
                .totalAdmins(userRepository.countByRole_Name("ADMIN"))
                .totalCompanies(companyRepository.count())
                .pendingCompanies(companyRepository.countByStatus(CompanyStatus.PENDING))
                .activeCompanies(companyRepository.countByStatus(CompanyStatus.ACTIVE))
                .totalJobs(jobRepository.count())
                .publishedJobs(jobRepository.countByStatus(JobStatus.PUBLISHED))
                .pendingJobs(jobRepository.countByStatus(JobStatus.PENDING))
                .flaggedJobs(jobRepository.countByStatus(JobStatus.FLAGGED))
                .rejectedJobs(jobRepository.countByStatus(JobStatus.REJECTED))
                .totalApplications(applicationRepository.count())
                .applicationsLast7Days(applicationRepository.countByAppliedAtAfter(now.minusDays(7)))
                .applicationsLast30Days(applicationRepository.countByAppliedAtAfter(now.minusDays(30)))
                .build();
    }

    private AdminDashboardResponse.CompanyQueueItem toCompanyQueueItem(Company company) {
        return AdminDashboardResponse.CompanyQueueItem.builder()
                .id(company.getId())
                .name(company.getName())
                .email(company.getEmail())
                .website(company.getWebsite())
                .industry(company.getIndustry())
                .status(company.getStatus())
                .ownerEmail(company.getCreatedBy() != null ? company.getCreatedBy().getEmail() : null)
                .build();
    }

    private AdminDashboardResponse.JobQueueItem toJobQueueItem(Job job) {
        Company company = job.getCompany();

        return AdminDashboardResponse.JobQueueItem.builder()
                .id(job.getId())
                .title(job.getTitle())
                .companyId(company != null ? company.getId() : null)
                .companyName(company != null ? company.getName() : null)
                .status(job.getStatus())
                .location(job.getLocation())
                .createdAt(job.getCreatedAt())
                .publishedAt(job.getPublishedAt())
                .build();
    }

    private AdminDashboardResponse.ApplicationActivityItem toApplicationActivityItem(Application application) {
        Job job = application.getJob();
        Company company = job != null ? job.getCompany() : null;

        return AdminDashboardResponse.ApplicationActivityItem.builder()
                .id(application.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .companyName(company != null ? company.getName() : null)
                .candidateName(application.getCandidate() != null ? application.getCandidate().getFullName() : null)
                .candidateEmail(application.getCandidate() != null && application.getCandidate().getUser() != null
                        ? application.getCandidate().getUser().getEmail()
                        : null)
                .aiScore(application.getAiScore())
                .appliedAt(application.getAppliedAt())
                .build();
    }

    private List<AdminDashboardResponse.ActivityItem> buildRecentActivity(
            List<Company> pendingCompanies,
            List<Job> pendingJobs,
            List<Application> recentApplications
    ) {
        List<AdminDashboardResponse.ActivityItem> activity = new ArrayList<>();

        pendingCompanies.forEach(company -> activity.add(AdminDashboardResponse.ActivityItem.builder()
                .type("COMPANY_VERIFICATION")
                .title("Company needs verification")
                .description(company.getName() + " is waiting for admin review.")
                .build()));

        pendingJobs.forEach(job -> activity.add(AdminDashboardResponse.ActivityItem.builder()
                .type("JOB_APPROVAL")
                .title("Job pending approval")
                .description(job.getTitle() + " is waiting for moderation.")
                .occurredAt(job.getCreatedAt())
                .build()));

        recentApplications.forEach(application -> {
            Job job = application.getJob();
            activity.add(AdminDashboardResponse.ActivityItem.builder()
                    .type("APPLICATION_SUBMITTED")
                    .title("New application submitted")
                    .description((application.getCandidate() != null ? application.getCandidate().getFullName() : "A candidate")
                            + " applied to "
                            + (job != null ? job.getTitle() : "a job")
                            + ".")
                    .occurredAt(application.getAppliedAt())
                    .build());
        });

        return activity.stream()
                .sorted(Comparator.comparing(
                        AdminDashboardResponse.ActivityItem::getOccurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(8)
                .toList();
    }
}
