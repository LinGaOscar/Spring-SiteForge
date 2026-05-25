package com.siteforge.cms.service;

import com.siteforge.cms.dto.PageContentRequest;
import com.siteforge.cms.dto.PageContentResponse;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.entity.PageContent;
import com.siteforge.domain.enums.TemplateKey;
import com.siteforge.domain.repository.PageContentRepository;
import com.siteforge.domain.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PageContentService {

    private final PageContentRepository pageContentRepository;
    private final PageRepository pageRepository;

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

    private static final Set<String> VALID_BODY_KEYS = Arrays.stream(TemplateKey.values())
            .filter(k -> k.name().contains("BODY"))
            .map(k -> k.name().toLowerCase())
            .collect(Collectors.toUnmodifiableSet());

    private void applyRequest(PageContent content, PageContentRequest request) {
        String key = request.getBlockKey() != null ? request.getBlockKey().toLowerCase() : null;
        if (key == null || !VALID_BODY_KEYS.contains(key))
            throw new IllegalArgumentException("blockKey 必須是合法的 Body TemplateKey，有效值：" + VALID_BODY_KEYS);
        content.setBlockKey(key);
        content.setSortOrder(request.getSortOrder());
        content.setContentJson(request.getContentJson());
        if (request.getLocale() != null && !request.getLocale().isBlank())
            content.setLocale(request.getLocale());
    }

    private PageContentResponse toResponse(PageContent c) {
        return new PageContentResponse(c.getId(), c.getPage().getId(), c.getBlockKey(),
            c.getSortOrder(), c.getContentJson(), c.getLocale(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
