package com.siteforge.domain.repository;

import com.siteforge.domain.entity.Page;
import com.siteforge.domain.enums.PageStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Long> {

    @EntityGraph(attributePaths = {"site", "layoutSet"})
    List<Page> findAll();

    @EntityGraph(attributePaths = {"site", "layoutSet"})
    Optional<Page> findById(Long id);

    Optional<Page> findBySiteIdAndPath(Long siteId, String path);

    List<Page> findBySiteIdAndStatus(Long siteId, PageStatus status);

    List<Page> findBySiteId(Long siteId);
}
