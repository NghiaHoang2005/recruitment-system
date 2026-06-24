package com.recruitment.backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.recruitment.backend.domain.dtos.CandidateProfileResponse;
import com.recruitment.backend.domain.dtos.OpenToWorkUpdateRequest;
import com.recruitment.backend.domain.dtos.ProfileCandidateUpdateRequest;
import com.recruitment.backend.domain.dtos.RecruiterProfileResponse;
import com.recruitment.backend.domain.dtos.RecruiterProfileUpdateRequest;
import com.recruitment.backend.domain.dtos.RegisterCandidateProfileRequest;
import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.entities.Candidate.CandidateSkill;
import com.recruitment.backend.domain.entities.CompanyMember;
import com.recruitment.backend.domain.entities.Recruiter;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.AccountType;
import com.recruitment.backend.domain.enums.JoinStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.ApplicationRepository;
import com.recruitment.backend.repositories.CandidateRepository;
import com.recruitment.backend.repositories.CandidateSkillRepository;
import com.recruitment.backend.repositories.CompanyMemberRepository;
import com.recruitment.backend.repositories.RecruiterRepository;
import com.recruitment.backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateRepository candidateRepository;
    private final SkillService skillService;
    private final CandidateSkillRepository candidateSkillRepository;
    private final UserRepository userRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final ApplicationRepository applicationRepository;
    private final RecruiterRepository recruiterRepository;
    private final Cloudinary cloudinary;

    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // ─── Candidate Profile ────────────────────────────────────────────────────

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
        Candidate candidate = candidateRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));
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

    @Transactional
    public CandidateProfileResponse updateAvatar(UUID userId, MultipartFile file) {
        validateAvatar(file);
        Candidate candidate = candidateRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "candidate_avatars",
                            "public_id", userId.toString(),
                            "resource_type", "image",
                            "overwrite", true,
                            "invalidate", true
                    )
            );
            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED);
            }

            candidate.setProfilePictureUrl(secureUrl.toString());
            candidateRepository.save(candidate);
            return getCandidateProfile(userId);
        } catch (IOException e) {
            throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED);
        }
    }

    // ─── Recruiter Profile ────────────────────────────────────────────────────

    @Transactional
    public RecruiterProfileResponse getRecruiterProfile(UUID userId) {
        Recruiter recruiter = recruiterRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RECRUITER_PROFILE_NOT_FOUND));

        Optional<CompanyMember> memberOpt = companyMemberRepository
                .findFirstByUser_IdAndJoinStatus(userId, JoinStatus.APPROVED);

        String companyName = memberOpt.map(m -> m.getCompany().getName()).orElse(null);
        String companyRole = memberOpt.map(m -> m.getRole() != null ? m.getRole().name() : null).orElse(null);

        return RecruiterProfileResponse.builder()
                .recruiterId(recruiter.getId())
                .fullName(recruiter.getFullName())
                .email(recruiter.getUser().getEmail())
                .phoneNumber(recruiter.getPhone())
                .profilePictureUrl(recruiter.getProfilePictureUrl())
                .headline(recruiter.getHeadline())
                .companyName(companyName)
                .companyRole(companyRole)
                .build();
    }

    @Transactional
    public RecruiterProfileResponse updateRecruiterProfile(UUID userId, RecruiterProfileUpdateRequest request) {
        Recruiter recruiter = recruiterRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RECRUITER_PROFILE_NOT_FOUND));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            recruiter.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            recruiter.setPhone(request.getPhoneNumber());
        }
        if (request.getHeadline() != null) {
            recruiter.setHeadline(request.getHeadline());
        }

        recruiterRepository.save(recruiter);
        return getRecruiterProfile(userId);
    }

    @Transactional
    public RecruiterProfileResponse updateRecruiterAvatar(UUID userId, MultipartFile file) {
        validateAvatar(file);
        Recruiter recruiter = recruiterRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RECRUITER_PROFILE_NOT_FOUND));

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "recruiter_avatars",
                            "public_id", userId.toString(),
                            "resource_type", "image",
                            "overwrite", true,
                            "invalidate", true
                    )
            );
            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED);
            }

            recruiter.setProfilePictureUrl(secureUrl.toString());
            recruiterRepository.save(recruiter);
            return getRecruiterProfile(userId);
        } catch (IOException e) {
            throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED);
        }
    }

    // ─── Shared Helpers ───────────────────────────────────────────────────────

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.AVATAR_FILE_EMPTY);
        }
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new AppException(ErrorCode.AVATAR_FILE_TOO_LARGE);
        }
        if (!ALLOWED_AVATAR_CONTENT_TYPES.contains(file.getContentType())) {
            throw new AppException(ErrorCode.AVATAR_INVALID_FILE_TYPE);
        }
    }
}
