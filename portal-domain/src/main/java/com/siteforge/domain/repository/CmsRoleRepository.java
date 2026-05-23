package com.siteforge.domain.repository;

import com.siteforge.domain.entity.CmsRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CmsRoleRepository extends JpaRepository<CmsRole, Long> {
    Optional<CmsRole> findByName(String name);
}
