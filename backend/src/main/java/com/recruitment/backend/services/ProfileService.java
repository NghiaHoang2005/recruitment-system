package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.ProfileCandidateUpdateRequest;
import com.recruitment.backend.domain.entities.Candidate;
import com.recruitment.backend.repositories.CandidateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateRepository candidateRepository;
    private final SkillService skillService;

    @Transactional
    public void confirmAndUpdateProfile(Long userId, ProfileCandidateUpdateRequest request) {
        Candidate candidate = candidateRepository.findById(userId).orElseThrow();
        candidate.setFullName(request.getFullName());
        candidate.setHeadline(request.getHeadline());
        candidate.setPhoneNumber(request.getPhoneNumber());
        skillService.linkSkillsToCandidate(userId, request.getConfirmedSkills());

        candidateRepository.save(candidate);
    }
}