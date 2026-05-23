package com.siteforge.cms.service;

import java.util.List;

public record PageSnapshot(
    Long pageId,
    String path,
    String title,
    String seoTitle,
    String seoDescription,
    Long layoutSetId,
    List<ContentBlock> contents
) {
    public record ContentBlock(
        String blockKey,
        int sortOrder,
        String contentJson,
        String locale
    ) {}
}
