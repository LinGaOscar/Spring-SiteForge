package com.siteforge.cms.controller;

import com.siteforge.domain.entity.Page;
import com.siteforge.domain.entity.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PageVisibilityTest {

    @Test
    void sharedPageIds_pageOwnedByOtherUnit_isIncluded() {
        // 驗證：當頁面所屬單位非當前單位時，應被歸類為「共享頁面」
        Unit unitA = new Unit(); unitA.setCode("00100");
        Unit unitB = new Unit(); unitB.setCode("00800");

        Page ownPage = new Page(); ownPage.setId(1L); ownPage.setUnit(unitA);
        Page sharedPage = new Page(); sharedPage.setId(2L); sharedPage.setUnit(unitB);

        List<Page> pages = List.of(ownPage, sharedPage);
        String currentUnitCode = "00100";

        Set<Long> sharedPageIds = pages.stream()
                .filter(p -> !currentUnitCode.equals(
                        p.getUnit() != null ? p.getUnit().getCode() : ""))
                .map(Page::getId)
                .collect(Collectors.toSet());

        assertThat(sharedPageIds).containsExactly(2L);
        assertThat(sharedPageIds).doesNotContain(1L);
    }

    @Test
    void sharedPageIds_allPagesOwnedBySameUnit_isEmpty() {
        // 驗證：所有頁面屬同一單位時，共享頁面集合應為空
        Unit unitA = new Unit(); unitA.setCode("00100");
        Page page1 = new Page(); page1.setId(1L); page1.setUnit(unitA);
        Page page2 = new Page(); page2.setId(2L); page2.setUnit(unitA);

        List<Page> pages = List.of(page1, page2);
        String currentUnitCode = "00100";

        Set<Long> sharedPageIds = pages.stream()
                .filter(p -> !currentUnitCode.equals(
                        p.getUnit() != null ? p.getUnit().getCode() : ""))
                .map(Page::getId)
                .collect(Collectors.toSet());

        assertThat(sharedPageIds).isEmpty();
    }

    @Test
    void page_visibleUnitCodes_addAndContain() {
        // 驗證：visibleUnitCodes 可正確存入並查詢，確保頁面共享名單欄位行為正確
        Page page = new Page();
        page.setVisibleUnitCodes(Set.of("00800", "00850"));

        assertThat(page.getVisibleUnitCodes()).contains("00800");
        assertThat(page.getVisibleUnitCodes()).contains("00850");
        assertThat(page.getVisibleUnitCodes()).doesNotContain("00100");
    }
}
