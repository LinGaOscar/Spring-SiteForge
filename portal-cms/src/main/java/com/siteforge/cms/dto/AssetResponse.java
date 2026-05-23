package com.siteforge.cms.dto;

import java.time.LocalDateTime;

public record AssetResponse(
    Long id,
    String filename,
    String filePath,
    String mimeType,
    Long size,
    LocalDateTime createdAt,
    String createdBy
) {}
