package com.siteforge.domain.repository;

import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.entity.Site;
import com.siteforge.domain.entity.Unit;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.enums.TemplateKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PageVisibilityRepositoryTest {

    @Autowired PageRepository pageRepository;
    @Autowired SiteRepository siteRepository;
    @Autowired LayoutSetRepository layoutSetRepository;
    @Autowired UnitRepository unitRepository;

    private Site site;
    private LayoutSet layoutSet;
    private Unit unitA;
    private Unit unitB;
    private Unit unitC;

    @BeforeEach
    void setUp() {
        site = new Site();
        site.setCode("test");
        site.setName("Test Site");
        siteRepository.save(site);

        layoutSet = new LayoutSet();
        layoutSet.setName("Default");
        layoutSet.setHeaderKey(TemplateKey.RWD_HEADER_01);
        layoutSet.setBodyKey(TemplateKey.RWD_BODY_01);
        layoutSet.setFooterKey(TemplateKey.RWD_FOOTER_01);
        layoutSetRepository.save(layoutSet);

        unitA = new Unit(); unitA.setCode("00100"); unitA.setName("A Unit");
        unitB = new Unit(); unitB.setCode("00800"); unitB.setName("B Unit");
        unitC = new Unit(); unitC.setCode("00850"); unitC.setName("C Unit");
        unitRepository.saveAll(List.of(unitA, unitB, unitC));
    }

    private Page makePage(String path, Unit owner, PageStatus status) {
        Page p = new Page();
        p.setSite(site); p.setLayoutSet(layoutSet);
        p.setPath(path); p.setTitle(path);
        p.setUnit(owner); p.setStatus(status);
        return p;
    }

    @Test
    void findByOwnerOrVisibleUnit_returnsOwnAndSharedPages() {
        // unitA owns pageA；pageB 是 unitB 的，但設定 unitA 可見
        Page pageA = makePage("/a", unitA, PageStatus.DRAFT);
        Page pageB = makePage("/b", unitB, PageStatus.DRAFT);
        pageB.setVisibleUnitCodes(Set.of("00100")); // 讓 unitA 可見
        pageRepository.saveAll(List.of(pageA, pageB));

        List<Page> result = pageRepository.findByOwnerOrVisibleUnit("00100");

        assertThat(result).extracting(Page::getPath).containsExactlyInAnyOrder("/a", "/b");
    }

    @Test
    void findByOwnerOrVisibleUnit_doesNotReturnUnrelatedPages() {
        // pageC 屬於 unitC，且未設定 unitA 可見
        Page pageA = makePage("/a", unitA, PageStatus.DRAFT);
        Page pageC = makePage("/c", unitC, PageStatus.DRAFT);
        pageRepository.saveAll(List.of(pageA, pageC));

        List<Page> result = pageRepository.findByOwnerOrVisibleUnit("00100");

        assertThat(result).extracting(Page::getPath).containsExactly("/a");
        assertThat(result).noneMatch(p -> "/c".equals(p.getPath()));
    }

    @Test
    void findByOwnerOrVisibleUnit_noDuplicatesWhenMultipleVisibleUnits() {
        // pageB 開放給 unitA 與 unitC 兩個單位，unitA 查詢時不應出現重複
        Page pageA = makePage("/a", unitA, PageStatus.DRAFT);
        Page pageB = makePage("/b", unitB, PageStatus.DRAFT);
        pageB.setVisibleUnitCodes(Set.of("00100", "00850"));
        pageRepository.saveAll(List.of(pageA, pageB));

        List<Page> result = pageRepository.findByOwnerOrVisibleUnit("00100");

        assertThat(result).extracting(Page::getPath).containsExactlyInAnyOrder("/a", "/b");
        assertThat(result).hasSize(2); // DISTINCT 確保無重複
    }

    @Test
    void findByOwnerOrVisibleUnitAndStatus_filtersStatusCorrectly() {
        Page draft = makePage("/draft", unitA, PageStatus.DRAFT);
        Page published = makePage("/pub", unitA, PageStatus.PUBLISHED);
        // unitB 的 PUBLISHED 頁面，開放 unitA 可見
        Page sharedPublished = makePage("/shared", unitB, PageStatus.PUBLISHED);
        sharedPublished.setVisibleUnitCodes(Set.of("00100"));
        pageRepository.saveAll(List.of(draft, published, sharedPublished));

        List<Page> drafts = pageRepository.findByOwnerOrVisibleUnitAndStatus("00100", PageStatus.DRAFT);
        List<Page> publishedPages = pageRepository.findByOwnerOrVisibleUnitAndStatus("00100", PageStatus.PUBLISHED);

        assertThat(drafts).extracting(Page::getPath).containsExactly("/draft");
        assertThat(publishedPages).extracting(Page::getPath).containsExactlyInAnyOrder("/pub", "/shared");
    }
}
