package com.siteforge.domain.repository;

import com.siteforge.domain.entity.PageVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PageVersionRepository extends JpaRepository<PageVersion, Long> {

    List<PageVersion> findByPageIdOrderByVersionNoDesc(Long pageId);

    @Query("SELECT MAX(pv.versionNo) FROM PageVersion pv WHERE pv.page.id = :pageId")
    Optional<Integer> findMaxVersionNoByPageId(@Param("pageId") Long pageId);
}
