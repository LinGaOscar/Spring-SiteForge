package com.siteforge.domain.repository;

import com.siteforge.domain.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {
    Optional<Site> findByCode(String code);
    List<Site> findByEnabledTrue();
}
