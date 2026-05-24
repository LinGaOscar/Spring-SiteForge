package com.siteforge.web.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.repository.PageContentRepository;
import com.siteforge.domain.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageRenderService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final PageRepository pageRepository;
    private final PageContentRepository pageContentRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<Page> findPublishedPage(Long siteId, String path) {
        return pageRepository.findBySiteIdAndPath(siteId, path)
                .filter(p -> p.getStatus() == PageStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<PageContentView> buildContentViews(Long pageId) {
        return pageContentRepository.findByPageIdOrderBySortOrder(pageId)
                .stream()
                .map(pc -> new PageContentView(
                        pc.getBlockKey(),
                        pc.getSortOrder(),
                        parseJson(pc.getContentJson())))
                .toList();
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            log.warn("無法解析 contentJson，略過此區塊: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
