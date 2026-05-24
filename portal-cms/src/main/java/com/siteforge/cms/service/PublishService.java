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
        if (!pageRepository.existsById(pageId))
            throw new IllegalArgumentException("Page not found: " + pageId);
        PageVersion targetVersion = pageVersionRepository.findById(versionId)
            .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        if (!targetVersion.getPage().getId().equals(pageId))
            throw new IllegalArgumentException("Version does not belong to page: " + pageId);

        Page managedPage = restoreSnapshot(pageId, targetVersion.getSnapshotJson());
        managedPage.setStatus(PageStatus.PUBLISHED);
        managedPage.setUpdatedBy(username);
        // 還原後清除工作流欄位，避免與 PUBLISHED 狀態不一致
        managedPage.setSubmittedBy(null);
        managedPage.setSubmittedAt(null);
        managedPage.setReviewNote(null);
        pageRepository.save(managedPage);

        List<PageContent> restoredContents = pageContentRepository.findByPageIdOrderBySortOrder(pageId);
        int nextVersionNo = pageVersionRepository.findMaxVersionNoByPageId(pageId).orElse(0) + 1;

        PageVersion rollbackVersion = new PageVersion();
        rollbackVersion.setPage(managedPage);
        rollbackVersion.setVersionNo(nextVersionNo);
        rollbackVersion.setSnapshotJson(buildSnapshot(managedPage, restoredContents));
        rollbackVersion.setStatus(PageStatus.PUBLISHED);
        rollbackVersion.setPublishedAt(LocalDateTime.now());
        rollbackVersion.setPublishedBy(username);

        return toVersionResponse(pageVersionRepository.save(rollbackVersion));
    }

    /** 由 WorkflowService 在 approvePublish 後呼叫，只建立 version 快照，不重複修改 page status */
    public void snapshotPublished(Long pageId, String publishedBy) {
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
        version.setPublishedBy(publishedBy);
        pageVersionRepository.save(version);
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

    private Page restoreSnapshot(Long pageId, String snapshotJson) {
        PageSnapshot snapshot;
        try {
            snapshot = objectMapper.readValue(snapshotJson, PageSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize page snapshot", e);
        }

        // 先刪除，避免 clearAutomatically=true 導致後續 page entity 變成 detached
        pageContentRepository.deleteAllByPageId(pageId);

        // EM.clear() 之後必須重新從 DB 取得受管理的 page entity
        Page managedPage = pageRepository.findById(pageId)
            .orElseThrow(() -> new IllegalStateException("Page not found after content delete: " + pageId));

        managedPage.setPath(snapshot.path());
        managedPage.setTitle(snapshot.title());
        managedPage.setSeoTitle(snapshot.seoTitle());
        managedPage.setSeoDescription(snapshot.seoDescription());
        if (snapshot.layoutSetId() != null) {
            managedPage.setLayoutSet(layoutSetRepository.getReferenceById(snapshot.layoutSetId()));
        } else {
            managedPage.setLayoutSet(null);
        }

        List<PageContent> restored = snapshot.contents().stream().map(block -> {
            PageContent c = new PageContent();
            c.setPage(managedPage);
            c.setBlockKey(block.blockKey());
            c.setSortOrder(block.sortOrder());
            c.setContentJson(block.contentJson());
            c.setLocale(block.locale());
            return c;
        }).toList();
        pageContentRepository.saveAll(restored);
        return managedPage;
    }

    private PageVersionResponse toVersionResponse(PageVersion v) {
        return new PageVersionResponse(
            v.getId(), v.getPage().getId(), v.getVersionNo(),
            v.getStatus(), v.getPublishedAt(), v.getPublishedBy(), v.getCreatedAt()
        );
    }
}
