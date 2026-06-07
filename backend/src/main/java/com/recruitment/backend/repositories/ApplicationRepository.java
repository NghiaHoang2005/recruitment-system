package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Application;
import com.recruitment.backend.domain.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    boolean existsByJob_IdAndCandidate_UserIdAndStatusNot(UUID jobId, UUID candidateId, ApplicationStatus status);
    boolean existsByCandidate_UserIdAndJob_Company_Id(UUID candidateId, UUID companyId);
    boolean existsByCv_IdAndJob_Company_Id(UUID cvId, UUID companyId);
    List<Application> findByCandidate_UserIdOrderByAppliedAtDesc(UUID candidateId);
    Optional<Application> findByIdAndCandidate_UserId(UUID id, UUID candidateId);
    List<Application> findByJob_Company_IdOrderByAppliedAtDesc(UUID companyId);
    List<Application> findByJob_IdAndJob_Company_IdOrderByAppliedAtDesc(UUID jobId, UUID companyId);
    Optional<Application> findByIdAndJob_Company_Id(UUID id, UUID companyId);
    long countByJob_Company_Id(UUID companyId);
    long countByJob_Company_IdAndAppliedAtAfter(UUID companyId, LocalDateTime appliedAt);
    long countByJob_Company_IdAndStatusIn(UUID companyId, Collection<ApplicationStatus> statuses);
    List<Application> findTop5ByJob_Company_IdOrderByAppliedAtDesc(UUID companyId);

    @Query("select count(distinct a.candidate.userId) from Application a where a.job.company.id = :companyId")
    long countDistinctCandidatesByCompanyId(@Param("companyId") UUID companyId);

    @Query("select avg(a.aiScore) from Application a where a.job.company.id = :companyId and a.aiScore is not null")
    Double averageAiScoreByCompanyId(@Param("companyId") UUID companyId);

    long countByJob_Id(UUID jobId);
    long countByJob_IdIn(Collection<UUID> jobIds);
    long countByAppliedAtAfter(LocalDateTime appliedAt);
    long countByAppliedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);
    long countByStatus(ApplicationStatus status);
    List<Application> findByAppliedAtAfter(LocalDateTime appliedAt);
    List<Application> findByAppliedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);
    List<Application> findTop5ByOrderByAppliedAtDesc();
}
