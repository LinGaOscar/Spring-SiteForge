package com.siteforge.domain.repository;

import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.entity.Site;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.enums.TemplateKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PageRepositoryTest {

    @Autowired PageRepository pageRepository;
    @Autowired SiteRepository siteRepository;
    @Autowired LayoutSetRepository layoutSetRepository;

    private Site site;
    private LayoutSet layoutSet;

    @BeforeEach
    void setUp() {
        site = new Site();
        site.setCode("test");
        site.setName("Test Site");
        siteRepository.save(site);

        layoutSet = new LayoutSet();
        layoutSet.setName("Default");
        layoutSet.setHeaderKey(TemplateKey.HEADER_DEFAULT);
        layoutSet.setBodyKey(TemplateKey.BODY_STANDARD);
        layoutSet.setFooterKey(TemplateKey.FOOTER_DEFAULT);
        layoutSetRepository.save(layoutSet);
    }

    @Test
    void saveAndFindBySiteIdAndPath() {
        Page page = new Page();
        page.setSite(site);
        page.setPath("/about");
        page.setTitle("About Us");
        page.setLayoutSet(layoutSet);
        pageRepository.save(page);

        assertThat(pageRepository.findBySiteIdAndPath(site.getId(), "/about"))
            .isPresent()
            .hasValueSatisfying(p -> {
                assertThat(p.getTitle()).isEqualTo("About Us");
                assertThat(p.getCreatedAt()).isNotNull();
            });
    }

    @Test
    void findBySiteIdAndStatus_filtersByStatus() {
        Page draft = new Page();
        draft.setSite(site); draft.setPath("/draft"); draft.setTitle("Draft");
        draft.setLayoutSet(layoutSet); draft.setStatus(PageStatus.DRAFT);
        pageRepository.save(draft);

        Page published = new Page();
        published.setSite(site); published.setPath("/pub"); published.setTitle("Published");
        published.setLayoutSet(layoutSet); published.setStatus(PageStatus.PUBLISHED);
        pageRepository.save(published);

        List<Page> publishedPages = pageRepository.findBySiteIdAndStatus(site.getId(), PageStatus.PUBLISHED);
        assertThat(publishedPages).hasSize(1);
        assertThat(publishedPages.get(0).getPath()).isEqualTo("/pub");

        List<Page> draftPages = pageRepository.findBySiteIdAndStatus(site.getId(), PageStatus.DRAFT);
        assertThat(draftPages).hasSize(1);
        assertThat(draftPages.get(0).getPath()).isEqualTo("/draft");
    }
}
