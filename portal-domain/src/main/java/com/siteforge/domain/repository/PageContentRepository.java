package com.siteforge.domain.repository;

import com.siteforge.domain.entity.PageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PageContentRepository extends JpaRepository<PageContent, Long> {

    List<PageContent> findByPageIdOrderBySortOrder(Long pageId);

    Optional<PageContent> findByPageIdAndBlockKeyAndLocale(Long pageId, String blockKey, String locale);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PageContent pc WHERE pc.page.id = :pageId")
    void deleteAllByPageId(@Param("pageId") Long pageId);
}
