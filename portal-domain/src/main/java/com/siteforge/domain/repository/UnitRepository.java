package com.siteforge.domain.repository;

import com.siteforge.domain.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, String> {
    List<Unit> findByEnabledTrue();
}
