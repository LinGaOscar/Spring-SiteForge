package com.siteforge.cms.service;

import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.entity.Unit;
import com.siteforge.domain.enums.CmsUserRole;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.enums.TemplateKey;
import com.siteforge.domain.repository.PageContentRepository;
import com.siteforge.domain.repository.PageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 跨單位操作安全測試：確保 CMS 使用者無法操作其他單位的頁面。
 * actor 屬於 unit-A（00100），pageB 屬於 unit-B（00800），所有操作應拋出 IllegalStateException。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowServiceUnitIsolationTest {

    @Mock PageRepository pageRepository;
    @Mock PageContentRepository pageContentRepository;
    @Mock CmsUserService userService;
    @Mock PublishService publishService;
    @InjectMocks WorkflowService workflowService;

    private CmsUser opA;   // unit 00100 的 OP
    private CmsUser maA;   // unit 00100 的 MA
    private Page pageB;    // unit 00800 的頁面

    @BeforeEach
    void setUp() {
        opA   = makeUser("op_a", "00100", CmsUserRole.OP);
        maA   = makeUser("ma_a", "00100", CmsUserRole.MA);
        pageB = makePage(1L, PageStatus.DRAFT, "00800");

        when(pageRepository.findById(1L)).thenReturn(Optional.of(pageB));
        // 使用 lenient：每個測試只呼叫單一 actor，另一個 stub 不被使用，避免 UnnecessaryStubbingException
        lenient().when(userService.unitCode(opA)).thenReturn("00100");
        lenient().when(userService.unitCode(maA)).thenReturn("00100");
        // pageB 屬於 00800，與 actor 的 00100 不同，checkUnit() 應擋下所有操作
    }

    // ── 輔助建構方法（與 WorkflowServiceTest 保持相同寫法）──────────────

    private CmsUser makeUser(String username, String unitCode, CmsUserRole... roles) {
        CmsUser u = new CmsUser();
        u.setUsername(username);
        Unit unit = new Unit();
        unit.setCode(unitCode);
        u.setUnit(unit);
        u.setRoles(new HashSet<>(Set.of(roles)));
        return u;
    }

    private Page makePage(Long id, PageStatus status, String unitCode) {
        Page p = new Page();
        p.setId(id);
        p.setStatus(status);
        Unit unit = new Unit();
        unit.setCode(unitCode);
        p.setUnit(unit);
        LayoutSet ls = new LayoutSet();
        ls.setHeaderKey(TemplateKey.RWD_HEADER_01);
        ls.setFooterKey(TemplateKey.RWD_FOOTER_01);
        p.setLayoutSet(ls);
        return p;
    }

    // ── 各工作流程操作：跨單位應被拒絕 ─────────────────────────────────

    @Test
    void submitForReview_differentUnit_throwsIllegalState() {
        assertThatThrownBy(() -> workflowService.submitForReview(1L, opA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("無權操作其他單位");
    }

    @Test
    void approveReview_differentUnit_throwsIllegalState() {
        pageB.setStatus(PageStatus.PENDING_REVIEW);
        assertThatThrownBy(() -> workflowService.approveReview(1L, maA, "LGTM"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("無權操作其他單位");
    }

    @Test
    void rejectReview_differentUnit_throwsIllegalState() {
        pageB.setStatus(PageStatus.PENDING_REVIEW);
        assertThatThrownBy(() -> workflowService.rejectReview(1L, maA, "格式不符"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("無權操作其他單位");
    }

    @Test
    void requestPublish_differentUnit_throwsIllegalState() {
        pageB.setStatus(PageStatus.APPROVED);
        assertThatThrownBy(() -> workflowService.requestPublish(1L, opA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("無權操作其他單位");
    }

    @Test
    void approvePublish_differentUnit_throwsIllegalState() {
        pageB.setStatus(PageStatus.PENDING_PUBLISH);
        assertThatThrownBy(() -> workflowService.approvePublish(1L, maA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("無權操作其他單位");
    }

    @Test
    void rejectPublish_differentUnit_throwsIllegalState() {
        pageB.setStatus(PageStatus.PENDING_PUBLISH);
        assertThatThrownBy(() -> workflowService.rejectPublish(1L, maA, "內容未完成"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("無權操作其他單位");
    }

    @Test
    void requestUnpublish_differentUnit_throwsIllegalState() {
        pageB.setStatus(PageStatus.PUBLISHED);
        assertThatThrownBy(() -> workflowService.requestUnpublish(1L, opA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("無權操作其他單位");
    }

    @Test
    void confirmUnpublish_differentUnit_throwsIllegalState() {
        pageB.setStatus(PageStatus.PENDING_UNPUBLISH);
        assertThatThrownBy(() -> workflowService.confirmUnpublish(1L, maA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("無權操作其他單位");
    }

    @Test
    void unpublish_differentUnit_throwsIllegalState() {
        pageB.setStatus(PageStatus.PUBLISHED);
        assertThatThrownBy(() -> workflowService.unpublish(1L, maA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("無權操作其他單位");
    }
}
