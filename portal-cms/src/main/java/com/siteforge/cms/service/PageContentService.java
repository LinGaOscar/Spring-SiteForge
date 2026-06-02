package com.siteforge.cms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siteforge.cms.dto.PageContentRequest;
import com.siteforge.cms.dto.PageContentResponse;
import com.siteforge.domain.entity.ComponentDefinition;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.entity.PageContent;
import com.siteforge.domain.repository.ComponentDefinitionRepository;
import com.siteforge.domain.repository.PageContentRepository;
import com.siteforge.domain.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PageContentService {

    private final PageContentRepository pageContentRepository;
    private final PageRepository pageRepository;
    private final ComponentDefinitionRepository componentDefinitionRepository;
    private final ObjectMapper objectMapper;
    private final HtmlSanitizerService htmlSanitizerService;

    @Transactional(readOnly = true)
    public List<PageContentResponse> findByPageId(Long pageId) {
        if (!pageRepository.existsById(pageId))
            throw new IllegalArgumentException("Page not found: " + pageId);
        return pageContentRepository.findByPageIdOrderBySortOrder(pageId)
            .stream().map(this::toResponse).toList();
    }

    public PageContentResponse create(Long pageId, PageContentRequest request) {
        Page page = pageRepository.findById(pageId)
            .orElseThrow(() -> new IllegalArgumentException("Page not found: " + pageId));
        PageContent content = new PageContent();
        content.setPage(page);
        applyRequest(content, request);
        return toResponse(pageContentRepository.save(content));
    }

    public PageContentResponse update(Long pageId, Long contentId, PageContentRequest request) {
        PageContent content = pageContentRepository.findById(contentId)
            .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));
        if (!content.getPage().getId().equals(pageId))
            throw new IllegalArgumentException("Content does not belong to page: " + pageId);
        applyRequest(content, request);
        return toResponse(pageContentRepository.save(content));
    }

    public void delete(Long pageId, Long contentId) {
        PageContent content = pageContentRepository.findById(contentId)
            .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));
        if (!content.getPage().getId().equals(pageId))
            throw new IllegalArgumentException("Content does not belong to page: " + pageId);
        pageContentRepository.deleteById(contentId);
    }

    private void applyRequest(PageContent content, PageContentRequest request) {
        String key = request.getBlockKey() != null ? request.getBlockKey().toLowerCase() : null;
        ComponentDefinition def = componentDefinitionRepository.findById(key)
            .filter(d -> Boolean.TRUE.equals(d.getActive()))
            .filter(d -> "BODY".equals(d.getType()))
            .orElseThrow(() -> new IllegalArgumentException("blockKey 不在已登記的元件清單中：" + key));
        content.setBlockKey(key);
        content.setSortOrder(request.getSortOrder());
        content.setContentJson(sanitizeRichtextFields(request.getContentJson(), def.getSchemaJson()));
        if (request.getLocale() != null && !request.getLocale().isBlank())
            content.setLocale(request.getLocale());
    }

    private String sanitizeRichtextFields(String contentJson, String schemaJson) {
        if (contentJson == null || contentJson.isBlank()) return contentJson;
        try {
            Set<String> richtextFields = parseRichtextFieldNames(schemaJson);
            if (richtextFields.isEmpty()) return contentJson;
            Map<String, Object> map = objectMapper.readValue(contentJson, new TypeReference<>() {});
            for (String field : richtextFields) {
                if (map.get(field) instanceof String raw) {
                    map.put(field, htmlSanitizerService.sanitize(raw));
                }
            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            // 解析失敗時保留原值，不影響儲存流程
            return contentJson;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> parseRichtextFieldNames(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) return Set.of();
        try {
            Map<String, Object> schema = objectMapper.readValue(schemaJson, new TypeReference<>() {});
            List<Map<String, Object>> fields = (List<Map<String, Object>>) schema.get("fields");
            if (fields == null) return Set.of();
            return fields.stream()
                .filter(f -> "richtext".equals(f.get("type")))
                .map(f -> (String) f.get("name"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            return Set.of();
        }
    }

    private PageContentResponse toResponse(PageContent c) {
        return new PageContentResponse(c.getId(), c.getPage().getId(), c.getBlockKey(),
            c.getSortOrder(), c.getContentJson(), c.getLocale(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
