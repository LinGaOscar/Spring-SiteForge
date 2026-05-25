package com.siteforge.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siteforge.cms.dto.PageVersionResponse;
import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.entity.PageVersion;
import com.siteforge.domain.entity.Site;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.enums.TemplateKey;
import com.siteforge.domain.repository.LayoutSetRepository;
import com.siteforge.domain.repository.PageContentRepository;
import com.siteforge.domain.repository.PageRepository;
import com.siteforge.domain.repository.PageVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishServiceTest {

    @Mock PageRepository pageRepository;
    @Mock PageContentRepository pageContentRepository;
    @Mock PageVersionRepository pageVersionRepository;
    @Spy ObjectMapper objectMapper;
    @Mock LayoutSetRepository layoutSetRepository;
    @InjectMocks PublishService publishService;

    private Page page;

    @BeforeEach
    void setUp() {
        Site site = new Site();
        site.setId(1L); site.setCode("default"); site.setName("Test");

        LayoutSet ls = new LayoutSet();
        ls.setId(1L); ls.setHeaderKey(TemplateKey.RWD_HEADER_01);
        ls.setFooterKey(TemplateKey.RWD_FOOTER_01);

        page = new Page();
        page.setId(1L); page.setSite(site); page.setPath("/about");
        page.setTitle("About"); page.setLayoutSet(ls);
        page.setStatus(PageStatus.DRAFT);
        page.setCreatedAt(LocalDateTime.now()); page.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void publish_draftPage_createsVersionAndUpdatesStatusToPublished() {
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        when(pageContentRepository.findByPageIdOrderBySortOrder(1L)).thenReturn(List.of());
        when(pageVersionRepository.findMaxVersionNoByPageId(1L)).thenReturn(Optional.empty());
        when(pageRepository.save(any())).thenReturn(page);

        PageVersion savedVersion = new PageVersion();
        savedVersion.setId(1L); savedVersion.setPage(page); savedVersion.setVersionNo(1);
        savedVersion.setSnapshotJson("{}"); savedVersion.setStatus(PageStatus.PUBLISHED);
        savedVersion.setPublishedAt(LocalDateTime.now()); savedVersion.setPublishedBy("manager");
        savedVersion.setCreatedAt(LocalDateTime.now());
        when(pageVersionRepository.save(any())).thenReturn(savedVersion);

        PageVersionResponse response = publishService.publish(1L, "manager");

        assertThat(response.versionNo()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(PageStatus.PUBLISHED);
        verify(pageRepository).save(argThat(p -> p.getStatus() == PageStatus.PUBLISHED));
    }

    @Test
    void listVersions_returnsVersionsForPage() {
        PageVersion v = new PageVersion();
        v.setId(1L); v.setPage(page); v.setVersionNo(1);
        v.setSnapshotJson("{}"); v.setStatus(PageStatus.PUBLISHED);
        v.setCreatedAt(LocalDateTime.now());
        when(pageVersionRepository.findByPageIdOrderByVersionNoDesc(1L)).thenReturn(List.of(v));

        assertThat(publishService.listVersions(1L)).hasSize(1);
    }

    @Test
    void rollback_validVersion_createsNewVersionAndRestoresPage() {
        when(pageRepository.existsById(1L)).thenReturn(true);

        PageVersion targetVersion = new PageVersion();
        targetVersion.setId(5L); targetVersion.setPage(page); targetVersion.setVersionNo(1);
        targetVersion.setSnapshotJson("{\"pageId\":1,\"path\":\"/about\",\"title\":\"About\",\"seoTitle\":null,\"seoDescription\":null,\"layoutSetId\":null,\"contents\":[]}");
        targetVersion.setStatus(PageStatus.PUBLISHED);
        targetVersion.setPublishedAt(LocalDateTime.now()); targetVersion.setPublishedBy("manager");
        targetVersion.setCreatedAt(LocalDateTime.now());
        when(pageVersionRepository.findById(5L)).thenReturn(Optional.of(targetVersion));

        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        when(pageContentRepository.findByPageIdOrderBySortOrder(1L)).thenReturn(List.of());
        when(pageVersionRepository.findMaxVersionNoByPageId(1L)).thenReturn(Optional.of(1));
        when(pageRepository.save(any())).thenReturn(page);

        PageVersion newVersion = new PageVersion();
        newVersion.setId(2L); newVersion.setPage(page); newVersion.setVersionNo(2);
        newVersion.setSnapshotJson("{}"); newVersion.setStatus(PageStatus.PUBLISHED);
        newVersion.setPublishedAt(LocalDateTime.now()); newVersion.setPublishedBy("manager");
        newVersion.setCreatedAt(LocalDateTime.now());
        when(pageVersionRepository.save(any())).thenReturn(newVersion);

        PageVersionResponse response = publishService.rollback(1L, 5L, "manager");
        assertThat(response.versionNo()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(PageStatus.PUBLISHED);
    }
}
