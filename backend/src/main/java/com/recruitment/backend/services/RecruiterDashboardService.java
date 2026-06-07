package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.RecruiterDashboardResponse;
import com.recruitment.backend.domain.entities.Company;
import com.recruitment.backend.domain.entities.CompanyMember;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.ApplicationStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.domain.enums.JoinStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.mappers.RecruiterDashboardMapper;
import com.recruitment.backend.repositories.ApplicationRepository;
import com.recruitment.backend.repositories.CompanyMemberRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecruiterDashboardService {
    private static final List<ApplicationStatus> INTERVIEW_STAGE_STATUSES = List.of(
            ApplicationStatus.INTERVIEW,
            ApplicationStatus.OFFERED,
            ApplicationStatus.HIRED
    );

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final RecruiterDashboardMapper recruiterDashboardMapper;
    private final SecurityUtil securityUtil;

    public RecruiterDashboardResponse getDashboard() {
        Company company = getRecruiterCompany();
        long activeJobs = jobRepository.countByCompany_IdAndStatusIn(company.getId(), List.of(JobStatus.PUBLISHED));
        long totalApplications = applicationRepository.countByJob_Company_Id(company.getId());
        long uniqueCandidates = applicationRepository.countDistinctCandidatesByCompanyId(company.getId());
        long applicationsLast7Days = applicationRepository.countByJob_Company_IdAndAppliedAtAfter(
                company.getId(),
                LocalDateTime.now().minusDays(7)
        );
        long interviewStageApplications = applicationRepository.countByJob_Company_IdAndStatusIn(
                company.getId(),
                INTERVIEW_STAGE_STATUSES
        );
        Double averageAiScore = applicationRepository.averageAiScoreByCompanyId(company.getId());

        return RecruiterDashboardResponse.builder()
                .stats(RecruiterDashboardResponse.Stats.builder()
                        .activeJobs(activeJobs)
                        .totalApplications(totalApplications)
                        .uniqueCandidates(uniqueCandidates)
                        .applicationsLast7Days(applicationsLast7Days)
                        .interviewStageApplications(interviewStageApplications)
                        .conversionRate(toPercent(interviewStageApplications, totalApplications))
                        .averageAiScore(averageAiScore == null ? null : (int) Math.round(averageAiScore))
                        .build())
                .recentApplications(applicationRepository.findTop5ByJob_Company_IdOrderByAppliedAtDesc(company.getId())
                        .stream()
                        .map(recruiterDashboardMapper::toRecentApplicationItem)
                        .toList())
                .build();
    }

    private Company getRecruiterCompany() {
        User user = securityUtil.getCurrentUser();
        CompanyMember membership = companyMemberRepository.findFirstByUser_IdAndJoinStatus(user.getId(), JoinStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_MEMBER_NOT_FOUND));
        return membership.getCompany();
    }

    private double toPercent(long value, long total) {
        if (total == 0) {
            return 0;
        }
        return Math.round((value * 1000.0) / total) / 10.0;
    }
}
