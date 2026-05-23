package com.siteforge.cms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siteforge.cms.dto.PageVersionResponse;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.entity.PageContent;
import com.siteforge.domain.entity.PageVersion;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.repository.PageContentRepository;
import com.siteforge.domain.repository.PageRepository;
import com.siteforge.domain.repository.LayoutSetRepository;
import com.siteforge.domain.repository.PageVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PublishService {

    private final PageRepository pageRepository;
    private final PageContentRepository pageContentRepository;
    private final PageVersionRepository pageVersionRepository;
    private final ObjectMapper objectMapper;
    private final LayoutSetRepository layoutSetRepository;

    public PageVersionResponse publish(Long pageId, String username) {
        Page page = pageRepository.findById(pageId)
            .orElseThrow(() -> new IllegalArgumentException("Page not found: " + pageId));

        List<PageContent> contents = pageContentRepository.findByPageIdOrderBySortOrder(pageId);
        int nextVersionNo = pageVersionRepository.findMaxVersionNoByPageId(pageId).orElse(0) + 1;

        PageVersion version = new PageVersion();
        version.setPage(page);
        version.setVersionNo(nextVersionNo);
        version.setSnapshotJson(buildSnapshot(page, contents));
        version.setStatus(PageStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());
        version.setPublishedBy(username);

        page.setStatus(PageStatus.PUBLISHED);
        pageRepository.save(page);

        return toVersionResponse(pageVersionRepository.save(version));
    }

    public PageVersionResponse rollback(Long pageId, Long versionId, String username) {
        Page page = pageRepository.findById(pageId)
            .orElseThrow(() -> new IllegalArgumentException("Page not found: " + pageId));
        PageVersion targetVersion = pageVersionRepository.findById(versionId)
            .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        if (!targetVersion.getPage().getId().equals(pageId))
            throw new IllegalArgumentException("Version does not belong to page: " + pageId);

        restoreSnapshot(page, targetVersion.getSnapshotJson());
        page.setStatus(PageStatus.PUBLISHED);
        page.setUpdatedBy(username);
        pageRepository.save(page);

        List<PageContent> restoredContents = pageContentRepository.findByPageIdOrderBySortOrder(pageId);
        int nextVersionNo = pageVersionRepository.findMaxVersionNoByPageId(pageId).orElse(0) + 1;

        PageVersion rollbackVersion = new PageVersion();
        rollbackVersion.setPage(page);
        rollbackVersion.setVersionNo(nextVersionNo);
        rollbackVersion.setSnapshotJson(buildSnapshot(page, restoredContents));
        rollbackVersion.setStatus(PageStatus.PUBLISHED);
        rollbackVersion.setPublishedAt(LocalDateTime.now());
        rollbackVersion.setPublishedBy(username);

        return toVersionResponse(pageVersionRepository.save(rollbackVersion));
    }

    @Transactional(readOnly = true)
    public List<PageVersionResponse> listVersions(Long pageId) {
        return pageVersionRepository.findByPageIdOrderByVersionNoDesc(pageId)
            .stream().map(this::toVersionResponse).toList();
    }

    private String buildSnapshot(Page page, List<PageContent> contents) {
        PageSnapshot snapshot = new PageSnapshot(
            page.getId(),
            page.getPath(),
            page.getTitle(),
            page.getSeoTitle(),
            page.getSeoDescription(),
            page.getLayoutSet() != null ? page.getLayoutSet().getId() : null,
            contents.stream().map(c -> new PageSnapshot.ContentBlock(
                c.getBlockKey(), c.getSortOrder(), c.getContentJson(), c.getLocale()
            )).toList()
        );
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize page snapshot", e);
        }
    }

    private void restoreSnapshot(Page page, String snapshotJson) {
        PageSnapshot snapshot;
        try {
            snapshot = objectMapper.readValue(snapshotJson, PageSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize page snapshot", e);
        }
        page.setPath(snapshot.path());
        page.setTitle(snapshot.title());
        page.setSeoTitle(snapshot.seoTitle());
        page.setSeoDescription(snapshot.seoDescription());
        if (snapshot.layoutSetId() != null) {
            page.setLayoutSet(layoutSetRepository.getReferenceById(snapshot.layoutSetId()));
        } else {
            page.setLayoutSet(null);
        }

        pageContentRepository.deleteAllByPageId(page.getId());
        List<PageContent> restored = snapshot.contents().stream().map(block -> {
            PageContent c = new PageContent();
            c.setPage(page);
            c.setBlockKey(block.blockKey());
            c.setSortOrder(block.sortOrder());
            c.setContentJson(block.contentJson());
            c.setLocale(block.locale());
            return c;
        }).toList();
        pageContentRepository.saveAll(restored);
    }

    private PageVersionResponse toVersionResponse(PageVersion v) {
        return new PageVersionResponse(
            v.getId(), v.getPage().getId(), v.getVersionNo(),
            v.getStatus(), v.getPublishedAt(), v.getPublishedBy(), v.getCreatedAt()
        );
    }
}
