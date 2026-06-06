package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.AdminSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminSettingRepository extends JpaRepository<AdminSetting, String> {
}
