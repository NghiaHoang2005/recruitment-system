package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.ProfileCandidateUpdateRequest;
import com.recruitment.backend.domain.dtos.CandidateProfileResponse;
import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.entities.Candidate.CandidateSkill;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CandidateRepository;
import com.recruitment.backend.repositories.CandidateSkillRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateRepository candidateRepository;
    private final SkillService skillService;
    private final CandidateSkillRepository candidateSkillRepository;

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
                .build();
    }

    @Transactional
    public void confirmAndUpdateProfile(UUID userId, ProfileCandidateUpdateRequest request) {
        Candidate candidate = candidateRepository.findById(userId).orElseThrow(()-> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));
        candidate.setFullName(request.getFullName());
        candidate.setHeadline(request.getHeadline());
        candidate.setPhoneNumber(request.getPhoneNumber());
        candidate.setOpenToWork(request.getOpenToWork());
        if (request.getConfirmedSkills() != null) {
            skillService.linkSkillsToCandidate(userId, request.getConfirmedSkills());
        }

        candidateRepository.save(candidate);
    }
}
