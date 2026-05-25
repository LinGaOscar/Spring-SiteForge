package com.siteforge.cms.service;

import com.siteforge.cms.dto.PageRequest;
import com.siteforge.cms.dto.PageResponse;
import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.entity.Site;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.enums.TemplateKey;
import com.siteforge.domain.repository.LayoutSetRepository;
import com.siteforge.domain.repository.PageRepository;
import com.siteforge.domain.repository.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PageServiceTest {

    @Mock PageRepository pageRepository;
    @Mock SiteRepository siteRepository;
    @Mock LayoutSetRepository layoutSetRepository;
    @InjectMocks PageService pageService;

    private Site site;
    private LayoutSet layoutSet;

    @BeforeEach
    void setUp() {
        site = new Site();
        site.setId(1L); site.setCode("default"); site.setName("Test");

        layoutSet = new LayoutSet();
        layoutSet.setId(1L); layoutSet.setName("Default");
        layoutSet.setHeaderKey(TemplateKey.RWD_HEADER_01);
        layoutSet.setFooterKey(TemplateKey.RWD_FOOTER_01);
    }

    @Test
    void create_validRequest_returnsPageResponse() {
        PageRequest request = new PageRequest();
        request.setSiteId(1L); request.setPath("/about");
        request.setTitle("About"); request.setLayoutSetId(1L);

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(layoutSetRepository.findById(1L)).thenReturn(Optional.of(layoutSet));
        when(pageRepository.findBySiteIdAndPath(1L, "/about")).thenReturn(Optional.empty());

        Page saved = new Page();
        saved.setId(1L); saved.setSite(site); saved.setPath("/about");
        saved.setTitle("About"); saved.setLayoutSet(layoutSet);
        saved.setStatus(PageStatus.DRAFT);
        saved.setCreatedAt(LocalDateTime.now()); saved.setUpdatedAt(LocalDateTime.now());
        when(pageRepository.save(any())).thenReturn(saved);

        PageResponse response = pageService.create(request, "manager");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.path()).isEqualTo("/about");
        assertThat(response.status()).isEqualTo(PageStatus.DRAFT);
    }

    @Test
    void create_duplicatePath_throwsIllegalArgument() {
        PageRequest request = new PageRequest();
        request.setSiteId(1L); request.setPath("/about");
        request.setTitle("About"); request.setLayoutSetId(1L);

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(layoutSetRepository.findById(1L)).thenReturn(Optional.of(layoutSet));
        when(pageRepository.findBySiteIdAndPath(1L, "/about")).thenReturn(Optional.of(new Page()));

        assertThatThrownBy(() -> pageService.create(request, "manager"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Path already exists");
    }

    @Test
    void delete_notFound_throwsIllegalArgument() {
        when(pageRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> pageService.delete(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page not found");
    }

    @Test
    void delete_existingPage_callsDeleteById() {
        when(pageRepository.existsById(1L)).thenReturn(true);

        pageService.delete(1L);
        verify(pageRepository).deleteById(1L);
    }
}
