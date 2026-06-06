package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.AdminCompanyResponse;
import com.recruitment.backend.domain.dtos.AdminPageResponse;
import com.recruitment.backend.domain.entities.Company;
import com.recruitment.backend.domain.entities.CompanyMember;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.enums.CompanyRole;
import com.recruitment.backend.domain.enums.CompanyStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.domain.enums.JoinStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.mappers.AdminMapper;
import com.recruitment.backend.notifications.domain.enums.NotificationType;
import com.recruitment.backend.notifications.services.NotificationFacade;
import com.recruitment.backend.repositories.CompanyMemberRepository;
import com.recruitment.backend.repositories.CompanyRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.repositories.RecruiterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final JobRepository jobRepository;
    private final RecruiterRepository recruiterRepository;
    private final AdminMapper adminMapper;
    private final AdminAuditLogService adminAuditLogService;
    private final NotificationFacade notificationFacade;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminPageResponse<AdminCompanyResponse> getCompanies(
            int page,
            int size,
            String keyword,
            CompanyStatus status
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "name")
        );
        Page<Company> companies = companyRepository.searchAdminCompanies(normalize(keyword), status, pageable);

        return AdminPageResponse.<AdminCompanyResponse>builder()
                .items(companies.stream().map(this::toCompanyResponse).toList())
                .page(companies.getNumber())
                .size(companies.getSize())
                .totalItems(companies.getTotalElements())
                .totalPages(companies.getTotalPages())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminCompanyResponse getCompany(UUID companyId) {
        return toCompanyResponse(findCompany(companyId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminCompanyResponse verifyCompany(UUID companyId, String reason) {
        Company company = findCompany(companyId);
        company.setStatus(CompanyStatus.ACTIVE);

        for (Job job : jobRepository.findByCompany_IdAndStatus(companyId, JobStatus.PENDING)) {
            job.setStatus(JobStatus.PUBLISHED);
            job.setPublishedAt(LocalDateTime.now());
            jobRepository.save(job);
        }

        Company savedCompany = companyRepository.save(company);
        adminAuditLogService.record("COMPANY_VERIFIED", "COMPANY", companyId, reason);
        notifyCompanyOwner(savedCompany, NotificationType.COMPANY_VERIFIED, reason, "company-verified:" + companyId);
        return toCompanyResponse(savedCompany);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminCompanyResponse rejectCompany(UUID companyId, String reason) {
        Company company = findCompany(companyId);
        company.setStatus(CompanyStatus.REJECTED);
        Company savedCompany = companyRepository.save(company);
        adminAuditLogService.record("COMPANY_REJECTED", "COMPANY", companyId, reason);
        notifyCompanyOwner(savedCompany, NotificationType.COMPANY_REJECTED, reason, "company-rejected:" + companyId);
        return toCompanyResponse(savedCompany);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminCompanyResponse requestMoreInfo(UUID companyId, String reason) {
        Company company = findCompany(companyId);
        company.setStatus(CompanyStatus.PENDING);
        Company savedCompany = companyRepository.save(company);
        adminAuditLogService.record("COMPANY_MORE_INFO_REQUESTED", "COMPANY", companyId, reason);
        notifyCompanyOwner(savedCompany, NotificationType.COMPANY_MORE_INFO_REQUESTED, reason, "company-more-info:" + companyId);
        return toCompanyResponse(savedCompany);
    }

    private Company findCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
    }

    private AdminCompanyResponse toCompanyResponse(Company company) {
        AdminCompanyResponse response = adminMapper.toCompanyResponse(company);
        UUID companyId = company.getId();
        response.setMemberCount(companyMemberRepository.countByCompany_IdAndJoinStatus(companyId, JoinStatus.APPROVED));
        response.setPendingMemberCount(companyMemberRepository.countByCompany_IdAndJoinStatus(companyId, JoinStatus.PENDING));
        response.setOpenJobCount(jobRepository.countByCompany_IdAndStatusIn(
                companyId,
                java.util.List.of(JobStatus.PENDING, JobStatus.PUBLISHED)
        ));

        companyMemberRepository.findFirstByCompany_IdAndRoleAndJoinStatus(companyId, CompanyRole.OWNER, JoinStatus.APPROVED)
                .map(CompanyMember::getUser)
                .ifPresent(owner -> {
                    response.setOwnerEmail(owner.getEmail());
                    recruiterRepository.findById(owner.getId()).ifPresent(recruiter -> response.setOwnerName(recruiter.getFullName()));
                });

        return response;
    }

    private void notifyCompanyOwner(Company company, NotificationType notificationType, String reason, String idempotencyKey) {
        String ownerEmail = company.getCreatedBy() != null ? company.getCreatedBy().getEmail() : null;
        if (ownerEmail == null || ownerEmail.isBlank()) {
            return;
        }
        try {
            notificationFacade.notifyCompanyModeration(ownerEmail, company.getName(), notificationType, reason, idempotencyKey);
        } catch (RuntimeException exception) {
            log.warn("Could not enqueue company moderation notification for company {}", company.getId(), exception);
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
