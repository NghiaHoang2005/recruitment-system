package com.recruitment.backend.mappers;

import com.recruitment.backend.domain.dtos.CompanyMemberResponse;
import com.recruitment.backend.domain.entities.CompanyMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyMemberMapper {
    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(
            target = "reviewedBy",
            expression = "java(companyMember.getReviewedBy() != null ? companyMember.getReviewedBy().getId().toString() : null)"
    )
    CompanyMemberResponse toCompanyMemberResponse(CompanyMember companyMember);
}
