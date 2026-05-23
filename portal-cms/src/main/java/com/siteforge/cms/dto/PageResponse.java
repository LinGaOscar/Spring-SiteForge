package com.siteforge.cms.dto;

import com.siteforge.domain.enums.PageStatus;

import java.time.LocalDateTime;

public record PageResponse(
    Long id,
    Long siteId,
    String path,
    String title,
    String seoTitle,
    String seoDescription,
    Long layoutSetId,
    PageStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy
) {}
