package com.siteforge.cms.dto;

import com.siteforge.domain.enums.TemplateKey;

import java.time.LocalDateTime;

public record LayoutSetResponse(
    Long id,
    String name,
    TemplateKey headerKey,
    TemplateKey bodyKey,
    TemplateKey footerKey,
    String description,
    Boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
