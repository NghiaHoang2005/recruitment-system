package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.RegisterRecruiterRequest;
import com.recruitment.backend.domain.dtos.RegisterRecruiterResponse;
import com.recruitment.backend.domain.entities.Recruiter;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.AccountType;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.mappers.RecruiterMapper;
import com.recruitment.backend.repositories.RecruiterRepository;
import com.recruitment.backend.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecruiterService {
    private final RecruiterRepository recruiterRepository;
    private final RecruiterMapper recruiterMapper;
    private final SecurityUtil securityUtil;

    public RegisterRecruiterResponse createRecruiterProfile(RegisterRecruiterRequest request){
        User user = securityUtil.getCurrentUser();

        if (!AccountType.RECRUITER.name().equals(user.getRole().getName())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (recruiterRepository.existsById(user.getId())) {
            throw new AppException(ErrorCode.RECRUITER_PROFILE_ALREADY_EXISTS);
        }

        Recruiter recruiter = Recruiter.builder()
                .user(user)
                .fullName(request.getFullName())
                .gender(request.getGender())
                .phone(request.getPhone())
                .position(request.getPosition())
                .build();
        recruiterRepository.save(recruiter);
        return recruiterMapper.toRegisterRecruiterResponse(recruiter);
    }
}
