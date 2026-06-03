package com.siteforge.domain.repository;

import com.siteforge.domain.entity.Page;
import com.siteforge.domain.enums.PageStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Long> {

    @EntityGraph(attributePaths = {"site", "layoutSet", "unit"})
    List<Page> findAll();

    @EntityGraph(attributePaths = {"site", "layoutSet", "unit"})
    Optional<Page> findById(Long id);

    @EntityGraph(attributePaths = {"site", "layoutSet", "unit"})
    Optional<Page> findBySiteIdAndPath(Long siteId, String path);

    @EntityGraph(attributePaths = {"site", "layoutSet", "unit"})
    List<Page> findBySiteIdAndStatus(Long siteId, PageStatus status);

    @EntityGraph(attributePaths = {"site", "layoutSet", "unit"})
    List<Page> findBySiteId(Long siteId);

    // 依單位查詢（用於 CMS 後台隔離顯示）
    @EntityGraph(attributePaths = {"site", "layoutSet", "unit"})
    List<Page> findByUnitCode(String unitCode);

    @EntityGraph(attributePaths = {"site", "layoutSet", "unit"})
    List<Page> findByUnitCodeAndStatus(String unitCode, PageStatus status);

    @EntityGraph(attributePaths = {"site", "layoutSet", "unit"})
    List<Page> findByUnitCodeAndStatusIn(String unitCode, List<PageStatus> statuses);

    // 待 MA 審核：同單位的特定狀態頁面
    @EntityGraph(attributePaths = {"site", "layoutSet", "unit"})
    List<Page> findByUnitCodeAndStatusInOrderBySubmittedAtDesc(
            String unitCode, List<PageStatus> statuses);

    // 查詢 owner 或 visible 的頁面（不篩選狀態，支援跨單位可見性）
    @Query("SELECT DISTINCT p FROM Page p LEFT JOIN p.visibleUnitCodes v " +
           "WHERE p.unit.code = :unitCode OR v = :unitCode")
    @EntityGraph(attributePaths = {"site", "layoutSet", "unit"})
    List<Page> findByOwnerOrVisibleUnit(@Param("unitCode") String unitCode);

    // 查詢 owner 或 visible 的頁面（篩選特定狀態，支援跨單位可見性）
    @Query("SELECT DISTINCT p FROM Page p LEFT JOIN p.visibleUnitCodes v " +
           "WHERE (p.unit.code = :unitCode OR v = :unitCode) AND p.status = :status")
    @EntityGraph(attributePaths = {"site", "layoutSet", "unit"})
    List<Page> findByOwnerOrVisibleUnitAndStatus(@Param("unitCode") String unitCode,
                                                  @Param("status") PageStatus status);
}
