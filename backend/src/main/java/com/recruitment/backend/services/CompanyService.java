package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.CompanyMemberResponse;
import com.recruitment.backend.domain.dtos.CompanyRequest;
import com.recruitment.backend.domain.dtos.CompanyResponse;
import com.recruitment.backend.domain.entities.Company;
import com.recruitment.backend.domain.entities.CompanyMember;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.CompanyRole;
import com.recruitment.backend.domain.enums.CompanyStatus;
import com.recruitment.backend.domain.enums.JoinStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.mappers.CompanyMapper;
import com.recruitment.backend.mappers.CompanyMemberMapper;
import com.recruitment.backend.repositories.CompanyMemberRepository;
import com.recruitment.backend.repositories.CompanyRepository;
import com.recruitment.backend.repositories.RecruiterRepository;
import com.recruitment.backend.utils.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final SecurityUtil securityUtil;
    private final CompanyMemberRepository companyMemberRepository;
    private final RecruiterRepository recruiterRepository;
    private final CompanyMemberMapper companyMemberMapper;

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public CompanyResponse createCompany(CompanyRequest companyRequest){
        User user = securityUtil.getCurrentUser();
        if(!recruiterRepository.existsById(user.getId())){
            throw new AppException(ErrorCode.RECRUITER_PROFILE_NOT_FOUND);
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
        if(companyMemberRepository.existsByUserAndJoinStatus(user, JoinStatus.APPROVED)){
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

        return companyMemberMapper.toCompanyMemberResponse(companyMember);
    }

    @Transactional
    @PreAuthorize("hasRole('RECRUITER')")
    public CompanyMemberResponse processRequest(String companyId, String userId, JoinStatus status){
        User user = securityUtil.getCurrentUser();

        if(!companyMemberRepository.existsByCompany_IdAndUser_IdAndRole(UUID.fromString(companyId), user.getId(), CompanyRole.OWNER)){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        CompanyMember companyMember = companyMemberRepository.findByCompany_IdAndUser_IdAndJoinStatus(UUID.fromString(companyId), UUID.fromString(userId), JoinStatus.PENDING).orElseThrow(() -> new AppException(ErrorCode.COMPANY_MEMBER_NOT_FOUND));


        companyMember.setJoinStatus(status);
        companyMember.setReviewedBy(user);
        companyMember.setReviewedAt(LocalDate.now());

        companyMemberRepository.save(companyMember);

        return companyMemberMapper.toCompanyMemberResponse(companyMember);
    }
}
