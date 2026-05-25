package com.siteforge.domain.repository;

import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.enums.TemplateKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LayoutSetRepository extends JpaRepository<LayoutSet, Long> {
    List<LayoutSet> findByEnabledTrue();
    Optional<LayoutSet> findByHeaderKeyAndFooterKey(TemplateKey headerKey, TemplateKey footerKey);
}
