package com.siteforge.domain.repository;

import com.siteforge.domain.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    // 全單位可瀏覽（用於素材選擇器）
    List<Asset> findAllByOrderByCreatedAtDesc();

    // 同單位才能管理（更新 / 刪除前確認）
    List<Asset> findByUnitCodeOrderByCreatedAtDesc(String unitCode);

    boolean existsByIdAndUnitCode(Long id, String unitCode);
}
