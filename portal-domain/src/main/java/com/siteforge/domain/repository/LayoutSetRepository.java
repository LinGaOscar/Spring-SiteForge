package com.siteforge.domain.repository;

import com.siteforge.domain.entity.LayoutSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LayoutSetRepository extends JpaRepository<LayoutSet, Long> {
    List<LayoutSet> findByEnabledTrue();
}
