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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock PageRepository pageRepository;
    @Mock PageContentRepository pageContentRepository;
    @Mock CmsUserService userService;
    @Mock PublishService publishService;
    @InjectMocks WorkflowService workflowService;

    private CmsUser opUser;
    private CmsUser maUser;

    @BeforeEach
    void setUp() {
        opUser = makeUser("op", "00100", CmsUserRole.OP);
        maUser = makeUser("ma", "00100", CmsUserRole.MA);
    }

    // ── 輔助建構方法 ──────────────────────────────────────

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

    /** 設定標準 OP mock：擁有 OP 角色、單位 00100 */
    private void stubOp() {
        when(userService.hasRole(opUser, CmsUserRole.OP)).thenReturn(true);
        when(userService.unitCode(opUser)).thenReturn("00100");
    }

    /** 設定標準 MA mock：擁有 MA 角色、單位 00100 */
    private void stubMa() {
        when(userService.hasRole(maUser, CmsUserRole.MA)).thenReturn(true);
        when(userService.unitCode(maUser)).thenReturn("00100");
    }

    // ── submitForReview ──────────────────────────────────

    @Test
    void submitForReview_happyPath_setsStatusToPendingReview() {
        Page page = makePage(1L, PageStatus.DRAFT, "00100");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubOp();
        // 頁面有至少一個 body 區塊
        when(pageContentRepository.findByPageIdOrderBySortOrder(1L)).thenReturn(List.of(mock(com.siteforge.domain.entity.PageContent.class)));

        workflowService.submitForReview(1L, opUser);

        assertThat(page.getStatus()).isEqualTo(PageStatus.PENDING_REVIEW);
        assertThat(page.getSubmittedBy()).isEqualTo("op");
    }

    @Test
    void submitForReview_actorIsMA_notOP_throwsIllegalState() {
        Page page = makePage(1L, PageStatus.DRAFT, "00100");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        // maUser 不具備 OP 角色 → checkRole 拋出
        when(userService.unitCode(maUser)).thenReturn("00100");
        when(userService.hasRole(maUser, CmsUserRole.OP)).thenReturn(false);

        assertThatThrownBy(() -> workflowService.submitForReview(1L, maUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("角色不符");
    }

    @Test
    void submitForReview_pageNotDraft_throwsIllegalState() {
        // APPROVED 狀態不允許送審
        Page page = makePage(1L, PageStatus.APPROVED, "00100");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubOp();

        assertThatThrownBy(() -> workflowService.submitForReview(1L, opUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("頁面狀態錯誤");
    }

    @Test
    void submitForReview_missingLayoutSet_throwsIllegalState() {
        // 沒有 layoutSet → checkPageComplete 拋出
        Page page = makePage(1L, PageStatus.DRAFT, "00100");
        page.setLayoutSet(null);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubOp();

        assertThatThrownBy(() -> workflowService.submitForReview(1L, opUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Header 與 Footer");
    }

    @Test
    void submitForReview_noContentBlocks_throwsIllegalState() {
        // 沒有 body 區塊 → checkPageComplete 拋出
        Page page = makePage(1L, PageStatus.DRAFT, "00100");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubOp();
        when(pageContentRepository.findByPageIdOrderBySortOrder(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> workflowService.submitForReview(1L, opUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Body 區塊");
    }

    // ── approveReview ──────────────────────────────────

    @Test
    void approveReview_happyPath_setsStatusToApproved() {
        Page page = makePage(1L, PageStatus.PENDING_REVIEW, "00100");
        page.setSubmittedBy("op"); // 非自審
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubMa();

        workflowService.approveReview(1L, maUser, "LGTM");

        assertThat(page.getStatus()).isEqualTo(PageStatus.APPROVED);
        assertThat(page.getReviewNote()).isEqualTo("LGTM");
    }

    @Test
    void approveReview_selfReview_throwsIllegalState() {
        // 送審人與審核人相同 → 自審防護
        Page page = makePage(1L, PageStatus.PENDING_REVIEW, "00100");
        page.setSubmittedBy("ma");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubMa();

        assertThatThrownBy(() -> workflowService.approveReview(1L, maUser, "note"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不能審核自己送出");
    }

    @Test
    void approveReview_wrongStatus_throwsIllegalState() {
        Page page = makePage(1L, PageStatus.DRAFT, "00100");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubMa();

        assertThatThrownBy(() -> workflowService.approveReview(1L, maUser, "note"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("頁面狀態錯誤");
    }

    // ── rejectReview ──────────────────────────────────

    @Test
    void rejectReview_happyPath_setsStatusBackToDraft() {
        Page page = makePage(1L, PageStatus.PENDING_REVIEW, "00100");
        page.setSubmittedBy("op");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubMa();

        workflowService.rejectReview(1L, maUser, "需修改");

        assertThat(page.getStatus()).isEqualTo(PageStatus.DRAFT);
        assertThat(page.getReviewNote()).isEqualTo("需修改");
    }

    // ── requestPublish ──────────────────────────────────

    @Test
    void requestPublish_happyPath_setsStatusToPendingPublish() {
        Page page = makePage(1L, PageStatus.APPROVED, "00100");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubOp();

        workflowService.requestPublish(1L, opUser);

        assertThat(page.getStatus()).isEqualTo(PageStatus.PENDING_PUBLISH);
        assertThat(page.getSubmittedBy()).isEqualTo("op");
    }

    // ── approvePublish ──────────────────────────────────

    @Test
    void approvePublish_happyPath_callsSnapshotAndSetsPublished() {
        Page page = makePage(1L, PageStatus.PENDING_PUBLISH, "00100");
        page.setSubmittedBy("op"); // 非自審
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubMa();

        workflowService.approvePublish(1L, maUser);

        assertThat(page.getStatus()).isEqualTo(PageStatus.PUBLISHED);
        // 驗證快照服務在同一交易內被呼叫
        verify(publishService).snapshotPublished(1L, "ma");
    }

    @Test
    void approvePublish_selfReview_throwsIllegalState() {
        Page page = makePage(1L, PageStatus.PENDING_PUBLISH, "00100");
        page.setSubmittedBy("ma"); // 自審
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubMa();

        assertThatThrownBy(() -> workflowService.approvePublish(1L, maUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不能審核自己送出");
    }

    // ── rejectPublish ──────────────────────────────────

    @Test
    void rejectPublish_happyPath_setsStatusToApproved() {
        Page page = makePage(1L, PageStatus.PENDING_PUBLISH, "00100");
        page.setSubmittedBy("op");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubMa();

        workflowService.rejectPublish(1L, maUser, "版面需調整");

        assertThat(page.getStatus()).isEqualTo(PageStatus.APPROVED);
        assertThat(page.getReviewNote()).isEqualTo("版面需調整");
    }

    // ── unpublish / confirmUnpublish / requestUnpublish ──

    @Test
    void unpublish_happyPath_setsStatusToApproved() {
        Page page = makePage(1L, PageStatus.PUBLISHED, "00100");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubMa();

        workflowService.unpublish(1L, maUser);

        assertThat(page.getStatus()).isEqualTo(PageStatus.APPROVED);
        assertThat(page.getUpdatedBy()).isEqualTo("ma");
    }

    @Test
    void requestUnpublish_happyPath_setsStatusToPendingUnpublish() {
        Page page = makePage(1L, PageStatus.PUBLISHED, "00100");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubOp();

        workflowService.requestUnpublish(1L, opUser);

        assertThat(page.getStatus()).isEqualTo(PageStatus.PENDING_UNPUBLISH);
        assertThat(page.getSubmittedBy()).isEqualTo("op");
    }

    @Test
    void confirmUnpublish_happyPath_setsStatusToApproved() {
        Page page = makePage(1L, PageStatus.PENDING_UNPUBLISH, "00100");
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));
        stubMa();

        workflowService.confirmUnpublish(1L, maUser);

        assertThat(page.getStatus()).isEqualTo(PageStatus.APPROVED);
        assertThat(page.getUpdatedBy()).isEqualTo("ma");
    }
}
