package com.siteforge.cms.service;

import com.siteforge.cms.dto.PageContentRequest;
import com.siteforge.cms.dto.PageContentResponse;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.entity.PageContent;
import com.siteforge.domain.repository.PageContentRepository;
import com.siteforge.domain.repository.PageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageContentServiceTest {

    @Mock PageContentRepository pageContentRepository;
    @Mock PageRepository pageRepository;
    @InjectMocks PageContentService pageContentService;

    @Test
    void create_validPageId_returnsSavedContent() {
        Page page = new Page();
        page.setId(1L);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));

        PageContentRequest request = new PageContentRequest();
        request.setBlockKey("hero");
        request.setSortOrder(0);
        request.setContentJson("{\"title\":\"Hello\"}");

        PageContent saved = new PageContent();
        saved.setId(1L); saved.setPage(page); saved.setBlockKey("hero");
        saved.setSortOrder(0); saved.setContentJson("{\"title\":\"Hello\"}");
        saved.setLocale("zh-TW");
        saved.setCreatedAt(LocalDateTime.now()); saved.setUpdatedAt(LocalDateTime.now());
        when(pageContentRepository.save(any())).thenReturn(saved);

        PageContentResponse response = pageContentService.create(1L, request);
        assertThat(response.blockKey()).isEqualTo("hero");
        assertThat(response.pageId()).isEqualTo(1L);
    }

    @Test
    void create_pageNotFound_throwsIllegalArgument() {
        when(pageRepository.findById(99L)).thenReturn(Optional.empty());
        PageContentRequest request = new PageContentRequest();
        request.setBlockKey("hero"); request.setContentJson("{}");

        assertThatThrownBy(() -> pageContentService.create(99L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page not found");
    }

    @Test
    void delete_contentBelongsToDifferentPage_throwsIllegalArgument() {
        Page otherPage = new Page();
        otherPage.setId(2L);
        PageContent content = new PageContent();
        content.setId(1L); content.setPage(otherPage);
        when(pageContentRepository.findById(1L)).thenReturn(Optional.of(content));

        assertThatThrownBy(() -> pageContentService.delete(1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Content does not belong to page");
    }
}
