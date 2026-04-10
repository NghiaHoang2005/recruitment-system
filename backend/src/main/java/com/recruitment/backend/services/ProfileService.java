package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.ProfileCandidateUpdateRequest;
import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CandidateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateRepository candidateRepository;
    private final SkillService skillService;

    @Transactional
    public void confirmAndUpdateProfile(UUID userId, ProfileCandidateUpdateRequest request) {
        Candidate candidate = candidateRepository.findById(userId).orElseThrow(()-> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));
        candidate.setFullName(request.getFullName());
        candidate.setHeadline(request.getHeadline());
        candidate.setPhoneNumber(request.getPhoneNumber());
        skillService.linkSkillsToCandidate(userId, request.getConfirmedSkills());

        candidateRepository.save(candidate);
    }
}