package com.siteforge.domain.repository;

import com.siteforge.domain.entity.CmsUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CmsUserRepository extends JpaRepository<CmsUser, Long> {
    Optional<CmsUser> findByUsername(String username);
}
