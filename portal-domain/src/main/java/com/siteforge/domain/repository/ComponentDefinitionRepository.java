package com.siteforge.domain.repository;

import com.siteforge.domain.entity.ComponentDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComponentDefinitionRepository extends JpaRepository<ComponentDefinition, String> {
    List<ComponentDefinition> findByTypeAndActiveTrue(String type);
    boolean existsByKeyAndTypeAndActiveTrue(String key, String type);
}
