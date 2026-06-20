package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findAllByOrderByDisplayOrderAsc();
    Optional<Location> findByCode(String code);
}
