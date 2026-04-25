package com.recruitment.backend.mappers;

import com.recruitment.backend.domain.dtos.CompanyRequest;
import com.recruitment.backend.domain.dtos.CompanyResponse;
import com.recruitment.backend.domain.entities.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyMapper {
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    Company toCompany(CompanyRequest request);

    @Mapping(target = "createdById", source = "createdBy.id")
    CompanyResponse toCompanyResponse(Company company);
}
