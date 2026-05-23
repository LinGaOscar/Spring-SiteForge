package com.siteforge.domain.repository;

import com.siteforge.domain.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findAllByOrderByCreatedAtDesc();
}
