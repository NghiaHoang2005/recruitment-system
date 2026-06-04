package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole_Name(String roleName);

    @Query("""
            select u from User u
            where (:role is null or u.role.name = :role)
              and (:enabled is null or u.enabled = :enabled)
              and (
                :keyword is null
                or lower(u.email) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<User> searchAdminUsers(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );
}
