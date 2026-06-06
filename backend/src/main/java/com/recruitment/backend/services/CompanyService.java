package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.CompanyMemberResponse;
import com.recruitment.backend.domain.dtos.CompanyDashboardResponse;
import com.recruitment.backend.domain.dtos.CompanyInviteRequest;
import com.recruitment.backend.domain.dtos.CompanyInviteResponse;
import com.recruitment.backend.domain.dtos.CompanyRequest;
import com.recruitment.backend.domain.dtos.CompanyResponse;
import com.recruitment.backend.domain.entities.Company;
import com.recruitment.backend.domain.entities.CompanyInvite;
import com.recruitment.backend.domain.entities.CompanyMember;
import com.recruitment.backend.domain.entities.Recruiter;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.CompanyRole;
import com.recruitment.backend.domain.enums.CompanyInviteStatus;
import com.recruitment.backend.domain.enums.CompanyStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.domain.enums.JoinStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.mappers.CompanyMapper;
import com.recruitment.backend.mappers.CompanyMemberMapper;
import com.recruitment.backend.notifications.domain.enums.NotificationType;
import com.recruitment.backend.notifications.services.NotificationFacade;
import com.recruitment.backend.repositories.CompanyMemberRepository;
import com.recruitment.backend.repositories.CompanyRepository;
import com.recruitment.backend.repositories.CompanyInviteRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.repositories.RecruiterRepository;
import com.recruitment.backend.repositories.UserRepository;
import com.recruitment.backend.utils.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final SecurityUtil securityUtil;
    private final CompanyMemberRepository companyMemberRepository;
    private final RecruiterRepository recruiterRepository;
    private final CompanyMemberMapper companyMemberMapper;
    private final JobRepository jobRepository;
    private final CompanyInviteRepository companyInviteRepository;
    private final UserRepository userRepository;
    private final NotificationFacade notificationFacade;

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public CompanyResponse createCompany(CompanyRequest companyRequest){
        User user = securityUtil.getCurrentUser();
        if(!recruiterRepository.existsById(user.getId())){
            throw new AppException(ErrorCode.RECRUITER_PROFILE_NOT_FOUND);
        }
        if(companyMemberRepository.existsByUserAndJoinStatusIn(user, Arrays.asList(JoinStatus.APPROVED, JoinStatus.PENDING))){
            throw new AppException(ErrorCode.RECRUITER_ALREADY_JOINED);
        }

        Company company = companyMapper.toCompany(companyRequest);

        company.setStatus(CompanyStatus.PENDING);

        company.setCreatedBy(user);

        companyRepository.save(company);

        CompanyMember companyMember = CompanyMember.builder()
                .company(company)
                .user(user)
                .joinStatus(JoinStatus.APPROVED)
                .role(CompanyRole.OWNER)
                .requestedAt(LocalDate.now())
                .reviewedAt(LocalDate.now())
                .reviewedBy(user)
                .build();

        companyMemberRepository.save(companyMember);
        notifyAdminsCompanyReviewRequested(company, user);

        return companyMapper.toCompanyResponse(company);
    }

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public CompanyMemberResponse joinCompany(String companyId) {
        Company company = companyRepository.findById(UUID.fromString(companyId)).orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        User user = securityUtil.getCurrentUser();
        if(!recruiterRepository.existsById(user.getId())){
            throw new AppException(ErrorCode.RECRUITER_PROFILE_NOT_FOUND);
        }

        if(companyMemberRepository.existsByCompanyAndUserAndJoinStatus(company, user, JoinStatus.PENDING)){
            throw new AppException(ErrorCode.COMPANY_MEMBER_EXISTED);
        }
        if(companyMemberRepository.existsByUserAndJoinStatusIn(user, Arrays.asList(JoinStatus.APPROVED, JoinStatus.PENDING))){
            throw new AppException(ErrorCode.RECRUITER_ALREADY_JOINED);
        }

        CompanyMember companyMember = CompanyMember.builder()
                .company(company)
                .user(user)
                .joinStatus(JoinStatus.PENDING)
                .role(CompanyRole.RECRUITER)
                .requestedAt(LocalDate.now())
                .build();

        companyMemberRepository.save(companyMember);

        return toCompanyMemberResponse(companyMember);
    }

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public CompanyMemberResponse processRequest(String companyId, String userId, JoinStatus status){
        User user = securityUtil.getCurrentUser();

        if(!companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndJoinStatus(UUID.fromString(companyId), user.getId(), CompanyRole.OWNER, JoinStatus.APPROVED)){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        CompanyMember companyMember = companyMemberRepository.findByCompany_IdAndUser_IdAndJoinStatus(UUID.fromString(companyId), UUID.fromString(userId), JoinStatus.PENDING).orElseThrow(() -> new AppException(ErrorCode.COMPANY_MEMBER_NOT_FOUND));


        companyMember.setJoinStatus(status);
        companyMember.setReviewedBy(user);
        companyMember.setReviewedAt(LocalDate.now());

        companyMemberRepository.save(companyMember);

        return toCompanyMemberResponse(companyMember);
    }

    @PreAuthorize("hasRole('RECRUITER')")
    public CompanyDashboardResponse getMyCompany() {
        User user = securityUtil.getCurrentUser();
        CompanyMember membership = companyMemberRepository.findFirstByUser_IdAndJoinStatus(user.getId(), JoinStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_MEMBER_NOT_FOUND));
        return toDashboardResponse(membership);
    }

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public CompanyDashboardResponse updateCompany(String companyId, CompanyRequest request) {
        User user = securityUtil.getCurrentUser();
        UUID parsedCompanyId = UUID.fromString(companyId);
        Company company = requireOwner(parsedCompanyId, user).getCompany();

        company.setName(request.getName());
        company.setWebsite(request.getWebsite());
        company.setEmail(request.getEmail());
        company.setPhone(request.getPhone());
        company.setAddress(request.getAddress());
        company.setCity(request.getCity());
        company.setCountry(request.getCountry());
        company.setDescription(request.getDescription());
        company.setIndustry(request.getIndustry());
        company.setCompanySize(request.getCompanySize());
        company.setTaxCode(request.getTaxCode());
        company.setBusinessLicense(request.getBusinessLicense());

        companyRepository.save(company);
        return toDashboardResponse(companyMemberRepository.findByCompany_IdAndUser_IdAndJoinStatus(parsedCompanyId, user.getId(), JoinStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_MEMBER_NOT_FOUND)));
    }

    @PreAuthorize("hasRole('RECRUITER')")
    public List<CompanyMemberResponse> getMyApprovedMemberships() {
        User user = securityUtil.getCurrentUser();
        return companyMemberRepository.findByUser_IdAndJoinStatus(user.getId(), JoinStatus.APPROVED)
                .stream()
                .map(this::toCompanyMemberResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('RECRUITER')")
    public List<CompanyMemberResponse> getCompanyMembers(String companyId) {
        User user = securityUtil.getCurrentUser();
        UUID parsedCompanyId = UUID.fromString(companyId);
        requireApprovedMember(parsedCompanyId, user);

        return companyMemberRepository.findByCompany_Id(parsedCompanyId)
                .stream()
                .filter(member -> member.getJoinStatus() == JoinStatus.APPROVED)
                .map(this::toCompanyMemberResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('RECRUITER')")
    public List<CompanyMemberResponse> getPendingRequests(String companyId) {
        User user = securityUtil.getCurrentUser();
        UUID parsedCompanyId = UUID.fromString(companyId);

        requireOwner(parsedCompanyId, user);

        return companyMemberRepository.findByCompany_IdAndJoinStatus(parsedCompanyId, JoinStatus.PENDING)
                .stream()
                .map(this::toCompanyMemberResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public void removeMember(String companyId, String userId) {
        User currentUser = securityUtil.getCurrentUser();
        UUID parsedCompanyId = UUID.fromString(companyId);
        UUID parsedUserId = UUID.fromString(userId);
        requireOwner(parsedCompanyId, currentUser);

        CompanyMember member = companyMemberRepository.findByCompany_IdAndUser_IdAndJoinStatus(parsedCompanyId, parsedUserId, JoinStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_MEMBER_NOT_FOUND));

        if (member.getRole() == CompanyRole.OWNER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        companyMemberRepository.delete(member);
    }

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public CompanyInviteResponse inviteRecruiter(String companyId, CompanyInviteRequest request) {
        User user = securityUtil.getCurrentUser();
        UUID parsedCompanyId = UUID.fromString(companyId);
        CompanyMember ownerMembership = requireOwner(parsedCompanyId, user);

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        String email = request.getEmail().trim().toLowerCase();
        if (companyInviteRepository.existsByCompany_IdAndEmailIgnoreCaseAndStatus(parsedCompanyId, email, CompanyInviteStatus.PENDING)) {
            throw new AppException(ErrorCode.COMPANY_MEMBER_EXISTED);
        }

        CompanyInvite invite = CompanyInvite.builder()
                .company(ownerMembership.getCompany())
                .email(email)
                .invitedBy(user)
                .status(CompanyInviteStatus.PENDING)
                .sentAt(LocalDate.now())
                .build();
        return toInviteResponse(companyInviteRepository.save(invite));
    }

    @PreAuthorize("hasRole('RECRUITER')")
    public List<CompanyInviteResponse> getInvites(String companyId) {
        User user = securityUtil.getCurrentUser();
        UUID parsedCompanyId = UUID.fromString(companyId);
        requireOwner(parsedCompanyId, user);

        return companyInviteRepository.findByCompany_IdOrderBySentAtDesc(parsedCompanyId)
                .stream()
                .map(this::toInviteResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public CompanyInviteResponse cancelInvite(String companyId, String inviteId) {
        User user = securityUtil.getCurrentUser();
        UUID parsedCompanyId = UUID.fromString(companyId);
        requireOwner(parsedCompanyId, user);

        CompanyInvite invite = companyInviteRepository.findById(UUID.fromString(inviteId))
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_MEMBER_NOT_FOUND));
        if (!invite.getCompany().getId().equals(parsedCompanyId) || invite.getStatus() != CompanyInviteStatus.PENDING) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        invite.setStatus(CompanyInviteStatus.CANCELLED);
        return toInviteResponse(companyInviteRepository.save(invite));
    }

    private CompanyMember requireOwner(UUID companyId, User user) {
        return companyMemberRepository.findByCompany_IdAndUser_IdAndJoinStatus(companyId, user.getId(), JoinStatus.APPROVED)
                .filter(member -> member.getRole() == CompanyRole.OWNER)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    }

    private CompanyMember requireApprovedMember(UUID companyId, User user) {
        return companyMemberRepository.findByCompany_IdAndUser_IdAndJoinStatus(companyId, user.getId(), JoinStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    }

    private CompanyDashboardResponse toDashboardResponse(CompanyMember membership) {
        Company company = membership.getCompany();
        UUID companyId = company.getId();
        long memberCount = companyMemberRepository.countByCompany_IdAndJoinStatus(companyId, JoinStatus.APPROVED);
        long pendingRequestCount = companyMemberRepository.countByCompany_IdAndJoinStatus(companyId, JoinStatus.PENDING);
        long openJobCount = jobRepository.countByCompany_IdAndStatusIn(companyId, Arrays.asList(JobStatus.PENDING, JobStatus.PUBLISHED));

        return CompanyDashboardResponse.builder()
                .companyId(companyId.toString())
                .name(company.getName())
                .website(company.getWebsite())
                .email(company.getEmail())
                .phone(company.getPhone())
                .address(company.getAddress())
                .city(company.getCity())
                .country(company.getCountry())
                .description(company.getDescription())
                .industry(company.getIndustry())
                .companySize(company.getCompanySize())
                .taxCode(company.getTaxCode())
                .businessLicense(company.getBusinessLicense())
                .status(company.getStatus())
                .currentUserCompanyRole(membership.getRole())
                .memberCount(memberCount)
                .openJobCount(openJobCount)
                .pipelineCandidateCount(0)
                .pendingRequestCount(pendingRequestCount)
                .build();
    }

    private CompanyInviteResponse toInviteResponse(CompanyInvite invite) {
        return CompanyInviteResponse.builder()
                .id(invite.getId().toString())
                .companyId(invite.getCompany().getId().toString())
                .email(invite.getEmail())
                .status(invite.getStatus())
                .sentAt(invite.getSentAt())
                .invitedBy(invite.getInvitedBy().getId().toString())
                .build();
    }

    private void notifyAdminsCompanyReviewRequested(Company company, User requester) {
        userRepository.findByRole_NameAndEnabledTrue("ADMIN").forEach(admin -> {
            try {
                notificationFacade.notifyAdminReviewRequested(
                        admin.getEmail(),
                        company.getName(),
                        requester.getEmail(),
                        NotificationType.ADMIN_COMPANY_REVIEW_REQUESTED,
                        "admin-company-review:" + company.getId() + ":" + admin.getId()
                );
            } catch (RuntimeException exception) {
                log.warn("Could not enqueue company review notification for admin {}", admin.getId(), exception);
            }
        });
    }

    private CompanyMemberResponse toCompanyMemberResponse(CompanyMember companyMember) {
        CompanyMemberResponse response = companyMemberMapper.toCompanyMemberResponse(companyMember);
        if (companyMember.getUser() != null) {
            recruiterRepository.findById(companyMember.getUser().getId())
                    .map(Recruiter::getFullName)
                    .ifPresent(response::setFullName);
        }
        return response;
    }
}

