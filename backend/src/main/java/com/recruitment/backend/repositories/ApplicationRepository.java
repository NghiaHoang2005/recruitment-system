package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Application;
import com.recruitment.backend.domain.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    boolean existsByJob_IdAndCandidate_UserIdAndStatusNot(UUID jobId, UUID candidateId, ApplicationStatus status);
    List<Application> findByCandidate_UserIdOrderByAppliedAtDesc(UUID candidateId);
    Optional<Application> findByIdAndCandidate_UserId(UUID id, UUID candidateId);
    List<Application> findByJob_Company_IdOrderByAppliedAtDesc(UUID companyId);
    List<Application> findByJob_IdAndJob_Company_IdOrderByAppliedAtDesc(UUID jobId, UUID companyId);
    Optional<Application> findByIdAndJob_Company_Id(UUID id, UUID companyId);
    long countByJob_Id(UUID jobId);
    long countByJob_IdIn(Collection<UUID> jobIds);
    long countByAppliedAtAfter(LocalDateTime appliedAt);
    List<Application> findTop5ByOrderByAppliedAtDesc();
}
