package com.siteforge.cms.service;

import com.siteforge.cms.dto.PageRequest;
import com.siteforge.cms.dto.PageResponse;
import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.entity.Site;
import com.siteforge.domain.repository.LayoutSetRepository;
import com.siteforge.domain.repository.PageRepository;
import com.siteforge.domain.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PageService {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LayoutSetRepository layoutSetRepository;

    @Transactional(readOnly = true)
    public List<PageResponse> findAll() {
        return pageRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse findById(Long id) {
        return pageRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new IllegalArgumentException("Page not found: " + id));
    }

    public PageResponse create(PageRequest request, String username) {
        Site site = siteRepository.findById(request.getSiteId())
            .orElseThrow(() -> new IllegalArgumentException("Site not found: " + request.getSiteId()));
        LayoutSet layoutSet = layoutSetRepository.findById(request.getLayoutSetId())
            .orElseThrow(() -> new IllegalArgumentException("LayoutSet not found: " + request.getLayoutSetId()));

        if (pageRepository.findBySiteIdAndPath(request.getSiteId(), request.getPath()).isPresent())
            throw new IllegalArgumentException("Path already exists: " + request.getPath());

        Page page = new Page();
        page.setSite(site);
        applyRequest(page, request, layoutSet);
        page.setCreatedBy(username);
        page.setUpdatedBy(username);
        return toResponse(pageRepository.save(page));
    }

    public PageResponse update(Long id, PageRequest request, String username) {
        Page page = pageRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Page not found: " + id));
        LayoutSet layoutSet = layoutSetRepository.findById(request.getLayoutSetId())
            .orElseThrow(() -> new IllegalArgumentException("LayoutSet not found: " + request.getLayoutSetId()));

        // 路徑變更時需確認新路徑不與同站其他頁面衝突
        if (!page.getPath().equals(request.getPath()) &&
            pageRepository.findBySiteIdAndPath(page.getSite().getId(), request.getPath()).isPresent())
            throw new IllegalArgumentException("Path already exists: " + request.getPath());

        applyRequest(page, request, layoutSet);
        page.setUpdatedBy(username);
        return toResponse(pageRepository.save(page));
    }

    public void delete(Long id) {
        if (!pageRepository.existsById(id))
            throw new IllegalArgumentException("Page not found: " + id);
        pageRepository.deleteById(id);
    }

    private void applyRequest(Page page, PageRequest request, LayoutSet layoutSet) {
        page.setPath(request.getPath());
        page.setTitle(request.getTitle());
        page.setSeoTitle(request.getSeoTitle());
        page.setSeoDescription(request.getSeoDescription());
        page.setLayoutSet(layoutSet);
    }

    private PageResponse toResponse(Page p) {
        return new PageResponse(
            p.getId(),
            p.getSite().getId(),
            p.getPath(),
            p.getTitle(),
            p.getSeoTitle(),
            p.getSeoDescription(),
            p.getLayoutSet() != null ? p.getLayoutSet().getId() : null,
            p.getStatus(),
            p.getCreatedAt(),
            p.getUpdatedAt(),
            p.getCreatedBy(),
            p.getUpdatedBy()
        );
    }
}
