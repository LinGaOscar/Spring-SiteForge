package com.siteforge.cms.dto;

import java.time.LocalDateTime;

public record PageContentResponse(
    Long id,
    Long pageId,
    String blockKey,
    int sortOrder,
    String contentJson,
    String locale,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
