package com.siteforge.web.service;

import java.util.Map;

/**
 * 將 page_content.content_json 解析後的 view 物件，供 Thymeleaf 直接存取 contentMap
 */
public record PageContentView(String blockKey, int sortOrder, Map<String, Object> contentMap) {
}
