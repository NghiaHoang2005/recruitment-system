package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.CompanyInvite;
import com.recruitment.backend.domain.enums.CompanyInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompanyInviteRepository extends JpaRepository<CompanyInvite, UUID> {
    List<CompanyInvite> findByCompany_IdOrderBySentAtDesc(UUID companyId);
    boolean existsByCompany_IdAndEmailIgnoreCaseAndStatus(UUID companyId, String email, CompanyInviteStatus status);
}
