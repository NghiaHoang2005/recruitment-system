package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Company;
import com.recruitment.backend.domain.enums.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    long countByStatus(CompanyStatus status);

    List<Company> findTop5ByStatusOrderByNameAsc(CompanyStatus status);

    @Query("""
            select c from Company c
            where (:status is null or c.status = :status)
              and (
                :keyword is null
                or lower(c.name) like lower(concat('%', :keyword, '%'))
                or lower(c.email) like lower(concat('%', :keyword, '%'))
                or lower(c.taxCode) like lower(concat('%', :keyword, '%'))
                or lower(c.industry) like lower(concat('%', :keyword, '%'))
                or lower(c.createdBy.email) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Company> searchAdminCompanies(
            @Param("keyword") String keyword,
            @Param("status") CompanyStatus status,
            Pageable pageable
    );
}
