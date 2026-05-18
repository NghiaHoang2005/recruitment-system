package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    Optional<Skill> findByNameIgnoreCase(String name);

    @Query("""
    SELECT s FROM Skill s
    WHERE LOWER(s.name) IN :names
    """)
    List<Skill> findAllByNameInIgnoreCase(List<String> names);
}
