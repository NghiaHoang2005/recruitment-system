package com.recruitment.backend.mappers;

import com.recruitment.backend.domain.dtos.RegisterRecruiterRequest;
import com.recruitment.backend.domain.dtos.RegisterRecruiterResponse;
import com.recruitment.backend.domain.entities.Recruiter;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecruiterMapper {
    RegisterRecruiterResponse toRegisterRecruiterResponse(Recruiter recruiter);
}
