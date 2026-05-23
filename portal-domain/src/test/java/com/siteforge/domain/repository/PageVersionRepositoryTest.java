package com.siteforge.domain.repository;

import com.siteforge.domain.entity.*;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.enums.TemplateKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PageVersionRepositoryTest {

    @Autowired PageVersionRepository pageVersionRepository;
    @Autowired PageRepository pageRepository;
    @Autowired SiteRepository siteRepository;
    @Autowired LayoutSetRepository layoutSetRepository;

    private Page page;

    @BeforeEach
    void setUp() {
        Site site = new Site();
        site.setCode("v-test"); site.setName("Test");
        siteRepository.save(site);

        LayoutSet ls = new LayoutSet();
        ls.setName("Default"); ls.setHeaderKey(TemplateKey.HEADER_DEFAULT);
        ls.setBodyKey(TemplateKey.BODY_STANDARD); ls.setFooterKey(TemplateKey.FOOTER_DEFAULT);
        layoutSetRepository.save(ls);

        page = new Page();
        page.setSite(site); page.setPath("/test"); page.setTitle("Test"); page.setLayoutSet(ls);
        pageRepository.save(page);
    }

    @Test
    void findMaxVersionNoByPageId_noVersions_returnsEmpty() {
        assertThat(pageVersionRepository.findMaxVersionNoByPageId(page.getId())).isEmpty();
    }

    @Test
    void findMaxVersionNoByPageId_returnsHighestVersion() {
        for (int i = 1; i <= 3; i++) {
            PageVersion v = new PageVersion();
            v.setPage(page); v.setVersionNo(i);
            v.setSnapshotJson("{}"); v.setStatus(PageStatus.PUBLISHED);
            pageVersionRepository.save(v);
        }
        assertThat(pageVersionRepository.findMaxVersionNoByPageId(page.getId())).contains(3);
    }

    @Test
    void findByPageIdOrderByVersionNoDesc_returnsNewestFirst() {
        for (int i = 1; i <= 3; i++) {
            PageVersion v = new PageVersion();
            v.setPage(page); v.setVersionNo(i);
            v.setSnapshotJson("{}"); v.setStatus(PageStatus.PUBLISHED);
            pageVersionRepository.save(v);
        }
        List<PageVersion> versions = pageVersionRepository.findByPageIdOrderByVersionNoDesc(page.getId());
        assertThat(versions).hasSize(3);
        assertThat(versions.get(0).getVersionNo()).isEqualTo(3);
        assertThat(versions.get(2).getVersionNo()).isEqualTo(1);
    }
}
