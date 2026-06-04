package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.AdminDashboardResponse;
import com.recruitment.backend.domain.entities.Application;
import com.recruitment.backend.domain.entities.Company;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.enums.CompanyStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.mappers.AdminMapper;
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
    private final AdminMapper adminMapper;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        List<Company> pendingCompanies = companyRepository.findTop5ByStatusOrderByNameAsc(CompanyStatus.PENDING);
        List<Job> pendingJobs = jobRepository.findTop5ByStatusOrderByCreatedAtDesc(JobStatus.PENDING);
        List<Job> recentJobs = jobRepository.findTop5ByOrderByCreatedAtDesc();
        List<Application> recentApplications = applicationRepository.findTop5ByOrderByAppliedAtDesc();

        return AdminDashboardResponse.builder()
                .metrics(buildMetrics())
                .pendingCompanies(pendingCompanies.stream().map(adminMapper::toCompanyQueueItem).toList())
                .pendingJobs(pendingJobs.stream().map(adminMapper::toJobQueueItem).toList())
                .recentJobs(recentJobs.stream().map(adminMapper::toJobQueueItem).toList())
                .recentApplications(recentApplications.stream().map(adminMapper::toApplicationActivityItem).toList())
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

    private List<AdminDashboardResponse.ActivityItem> buildRecentActivity(
            List<Company> pendingCompanies,
            List<Job> pendingJobs,
            List<Application> recentApplications
    ) {
        List<AdminDashboardResponse.ActivityItem> activity = new ArrayList<>();

        pendingCompanies.forEach(company -> activity.add(AdminDashboardResponse.ActivityItem.builder()
                .type("COMPANY_VERIFICATION")
                .title("Công ty cần xác minh")
                .description(company.getName() + " đang chờ admin duyệt.")
                .build()));

        pendingJobs.forEach(job -> activity.add(AdminDashboardResponse.ActivityItem.builder()
                .type("JOB_APPROVAL")
                .title("Tin tuyển dụng chờ duyệt")
                .description(job.getTitle() + " đang chờ kiểm duyệt.")
                .occurredAt(job.getCreatedAt())
                .build()));

        recentApplications.forEach(application -> {
            Job job = application.getJob();
            activity.add(AdminDashboardResponse.ActivityItem.builder()
                    .type("APPLICATION_SUBMITTED")
                    .title("Có đơn ứng tuyển mới")
                    .description((application.getCandidate() != null ? application.getCandidate().getFullName() : "A candidate")
                            + " đã ứng tuyển vào "
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
