package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.CandidateProfileResponse;
import com.recruitment.backend.domain.dtos.OpenToWorkUpdateRequest;
import com.recruitment.backend.domain.dtos.ProfileCandidateUpdateRequest;
import com.recruitment.backend.domain.dtos.RegisterCandidateProfileRequest;
import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.entities.Candidate.CandidateSkill;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.AccountType;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CandidateRepository;
import com.recruitment.backend.repositories.CandidateSkillRepository;
import com.recruitment.backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.recruitment.backend.domain.entities.CompanyMember;
import com.recruitment.backend.domain.enums.JoinStatus;
import com.recruitment.backend.repositories.ApplicationRepository;
import com.recruitment.backend.repositories.CompanyMemberRepository;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateRepository candidateRepository;
    private final SkillService skillService;
    private final CandidateSkillRepository candidateSkillRepository;
    private final UserRepository userRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional
    public CandidateProfileResponse getCandidateProfile(UUID userId) {
        Candidate candidate = candidateRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));

        List<CandidateSkill> mappings = candidateSkillRepository.findByCandidateUserId(userId);
        List<String> skills = mappings.stream()
                .map(mapping -> mapping.getSkill().getName())
                .collect(Collectors.toList());

        return CandidateProfileResponse.builder()
                .candidateId(candidate.getUserId())
                .fullName(candidate.getFullName())
                .headline(candidate.getHeadline())
                .phoneNumber(candidate.getPhoneNumber())
                .profilePictureUrl(candidate.getProfilePictureUrl())
                .openToWork(candidate.getOpenToWork())
                .skills(skills)
                .email(candidate.getUser().getEmail())
                .build();
    }

    @Transactional
    public CandidateProfileResponse getPublicCandidateProfile(UUID currentUserId, UUID candidateId) {
        if (currentUserId.equals(candidateId)) {
            return getCandidateProfile(candidateId);
        }

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));

        Optional<CompanyMember> memberOpt = companyMemberRepository.findFirstByUser_IdAndJoinStatus(currentUserId, JoinStatus.APPROVED);
        if (memberOpt.isPresent()) {
            UUID companyId = memberOpt.get().getCompany().getId();
            if (Boolean.TRUE.equals(candidate.getOpenToWork())) {
                return getCandidateProfile(candidateId);
            }
            if (applicationRepository.existsByCandidate_UserIdAndJob_Company_Id(candidateId, companyId)) {
                return getCandidateProfile(candidateId);
            }
        }

        throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    @Transactional
    public CandidateProfileResponse createCandidateProfile(UUID userId, RegisterCandidateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!AccountType.CANDIDATE.name().equals(user.getRole().getName())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (candidateRepository.existsById(userId)) {
            throw new AppException(ErrorCode.CANDIDATE_PROFILE_ALREADY_EXISTS);
        }

        Candidate candidate = Candidate.builder()
                .userId(user.getId())
                .user(user)
                .fullName(request.getFullName())
                .openToWork(false)
                .build();

        candidateRepository.save(candidate);
        return getCandidateProfile(userId);
    }

    @Transactional
    public CandidateProfileResponse confirmAndUpdateProfile(UUID userId, ProfileCandidateUpdateRequest request) {
        Candidate candidate = candidateRepository.findById(userId).orElseThrow(()-> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));
        candidate.setFullName(request.getFullName());
        candidate.setHeadline(request.getHeadline());
        candidate.setPhoneNumber(request.getPhoneNumber());
        candidate.setOpenToWork(request.getOpenToWork());
        if (request.getConfirmedSkills() != null) {
            skillService.linkSkillsToCandidate(userId, request.getConfirmedSkills());
        }

        candidateRepository.save(candidate);
        return getCandidateProfile(userId);
    }

    @Transactional
    public CandidateProfileResponse updateOpenToWork(UUID userId, OpenToWorkUpdateRequest request) {
        Candidate candidate = candidateRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));

        candidate.setOpenToWork(Boolean.TRUE.equals(request.getOpenToWork()));
        candidateRepository.save(candidate);

        return getCandidateProfile(userId);
    }
}
