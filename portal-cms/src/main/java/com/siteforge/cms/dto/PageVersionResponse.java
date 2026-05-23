package com.siteforge.cms.dto;

import com.siteforge.domain.enums.PageStatus;

import java.time.LocalDateTime;

public record PageVersionResponse(
    Long id,
    Long pageId,
    int versionNo,
    PageStatus status,
    LocalDateTime publishedAt,
    String publishedBy,
    LocalDateTime createdAt
) {}
