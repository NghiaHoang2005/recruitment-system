package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Company;
import com.recruitment.backend.domain.entities.CompanyMember;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.CompanyRole;
import com.recruitment.backend.domain.enums.JoinStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyMemberRepository extends JpaRepository<CompanyMember, UUID> {
    boolean existsByCompanyAndUserAndJoinStatus(Company company, User user, JoinStatus status);
    boolean existsByUserAndJoinStatus(User user, JoinStatus status);
    boolean existsByUserAndJoinStatusIn(User user, List<JoinStatus> statuses);
    boolean existsByCompany_IdAndUser_IdAndRole(UUID companyId, UUID userId, CompanyRole role);
    boolean existsByCompany_IdAndUser_IdAndRoleAndJoinStatus(UUID companyId, UUID userId, CompanyRole role, JoinStatus status);
    boolean existsByCompany_IdAndUser_IdAndJoinStatus(UUID companyId, UUID userId, JoinStatus status);
    List<CompanyMember> findByUser_IdAndJoinStatus(UUID userId, JoinStatus status);
    Optional<CompanyMember> findFirstByUser_IdAndJoinStatus(UUID userId, JoinStatus status);
    List<CompanyMember> findByCompany_Id(UUID companyId);
    List<CompanyMember> findByCompany_IdAndJoinStatus(UUID companyId, JoinStatus status);
    long countByCompany_IdAndJoinStatus(UUID companyId, JoinStatus status);
    Optional<CompanyMember> findByCompany_IdAndUser_IdAndJoinStatus(UUID companyId, UUID userId, JoinStatus status);
}
