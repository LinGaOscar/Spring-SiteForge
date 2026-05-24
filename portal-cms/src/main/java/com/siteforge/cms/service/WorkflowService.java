package com.siteforge.cms.service;

import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.enums.CmsUserRole;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowService {

    private final PageRepository pageRepository;
    private final CmsUserService userService;

    /** OP 送審：DRAFT → PENDING_REVIEW */
    public void submitForReview(Long pageId, CmsUser actor) {
        Page page = loadPage(pageId);
        checkUnit(page, actor);
        checkRole(actor, CmsUserRole.OP);
        checkStatus(page, PageStatus.DRAFT);

        page.setStatus(PageStatus.PENDING_REVIEW);
        page.setSubmittedAt(LocalDateTime.now());
        page.setSubmittedBy(actor.getUsername());
        page.setReviewNote(null);
    }

    /** MA 放行建立：PENDING_REVIEW → APPROVED */
    public void approveReview(Long pageId, CmsUser actor, String note) {
        Page page = loadPage(pageId);
        checkUnit(page, actor);
        checkRole(actor, CmsUserRole.MA);
        checkStatus(page, PageStatus.PENDING_REVIEW);
        checkNotSelf(page, actor);

        page.setStatus(PageStatus.APPROVED);
        page.setReviewNote(note);
    }

    /** MA 退回建立：PENDING_REVIEW → DRAFT */
    public void rejectReview(Long pageId, CmsUser actor, String note) {
        Page page = loadPage(pageId);
        checkUnit(page, actor);
        checkRole(actor, CmsUserRole.MA);
        checkStatus(page, PageStatus.PENDING_REVIEW);
        checkNotSelf(page, actor);

        page.setStatus(PageStatus.DRAFT);
        page.setReviewNote(note);
    }

    /** OP 申請發布：APPROVED → PENDING_PUBLISH */
    public void requestPublish(Long pageId, CmsUser actor) {
        Page page = loadPage(pageId);
        checkUnit(page, actor);
        checkRole(actor, CmsUserRole.OP);
        checkStatus(page, PageStatus.APPROVED);

        page.setStatus(PageStatus.PENDING_PUBLISH);
        page.setSubmittedAt(LocalDateTime.now());
        page.setSubmittedBy(actor.getUsername());
        page.setReviewNote(null);
    }

    /** MA 放行發布：PENDING_PUBLISH → PUBLISHED */
    public void approvePublish(Long pageId, CmsUser actor) {
        Page page = loadPage(pageId);
        checkUnit(page, actor);
        checkRole(actor, CmsUserRole.MA);
        checkStatus(page, PageStatus.PENDING_PUBLISH);
        checkNotSelf(page, actor);

        page.setStatus(PageStatus.PUBLISHED);
        page.setUpdatedBy(actor.getUsername());
    }

    /** MA 退回發布申請：PENDING_PUBLISH → APPROVED */
    public void rejectPublish(Long pageId, CmsUser actor, String note) {
        Page page = loadPage(pageId);
        checkUnit(page, actor);
        checkRole(actor, CmsUserRole.MA);
        checkStatus(page, PageStatus.PENDING_PUBLISH);
        checkNotSelf(page, actor);

        page.setStatus(PageStatus.APPROVED);
        page.setReviewNote(note);
    }

    /** OP 申請下架：PUBLISHED → PENDING_UNPUBLISH */
    public void requestUnpublish(Long pageId, CmsUser actor) {
        Page page = loadPage(pageId);
        checkUnit(page, actor);
        checkRole(actor, CmsUserRole.OP);
        checkStatus(page, PageStatus.PUBLISHED);

        page.setStatus(PageStatus.PENDING_UNPUBLISH);
        page.setSubmittedAt(LocalDateTime.now());
        page.setSubmittedBy(actor.getUsername());
    }

    /** MA 確認下架：PENDING_UNPUBLISH → APPROVED */
    public void confirmUnpublish(Long pageId, CmsUser actor) {
        Page page = loadPage(pageId);
        checkUnit(page, actor);
        checkRole(actor, CmsUserRole.MA);
        checkStatus(page, PageStatus.PENDING_UNPUBLISH);

        page.setStatus(PageStatus.APPROVED);
        page.setUpdatedBy(actor.getUsername());
    }

    /** MA 直接下架：PUBLISHED → APPROVED */
    public void unpublish(Long pageId, CmsUser actor) {
        Page page = loadPage(pageId);
        checkUnit(page, actor);
        checkRole(actor, CmsUserRole.MA);
        checkStatus(page, PageStatus.PUBLISHED);

        page.setStatus(PageStatus.APPROVED);
        page.setUpdatedBy(actor.getUsername());
    }

    // ── 內部輔助方法 ──────────────────────────────────────

    private Page loadPage(Long pageId) {
        return pageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Page not found: " + pageId));
    }

    private void checkUnit(Page page, CmsUser actor) {
        String pageUnit = page.getUnit() != null ? page.getUnit().getCode() : null;
        String actorUnit = userService.unitCode(actor);
        if (pageUnit == null || !pageUnit.equals(actorUnit)) {
            throw new IllegalStateException("無權操作其他單位的頁面");
        }
    }

    private void checkRole(CmsUser actor, CmsUserRole required) {
        if (!userService.hasRole(actor, required)) {
            throw new IllegalStateException("角色不符：需要 " + required.name());
        }
    }

    private void checkStatus(Page page, PageStatus expected) {
        if (page.getStatus() != expected) {
            throw new IllegalStateException(
                "頁面狀態錯誤，目前：" + page.getStatus() + "，需要：" + expected);
        }
    }

    private void checkNotSelf(Page page, CmsUser actor) {
        if (actor.getUsername().equals(page.getSubmittedBy())) {
            throw new IllegalStateException("不能審核自己送出的申請");
        }
    }
}
