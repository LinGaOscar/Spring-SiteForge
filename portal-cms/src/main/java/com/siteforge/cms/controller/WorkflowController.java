package com.siteforge.cms.controller;

import com.siteforge.domain.entity.CmsUser;
import com.siteforge.cms.service.CmsUserService;
import com.siteforge.cms.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pages/{id}")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final CmsUserService cmsUserService;

    @PostMapping("/submit")
    public String submit(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra) {
        return execute(id, ud, ra, (page, actor) -> workflowService.submitForReview(id, actor));
    }

    @PostMapping("/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String note,
                          @AuthenticationPrincipal UserDetails ud,
                          RedirectAttributes ra) {
        return execute(id, ud, ra, (page, actor) -> workflowService.approveReview(id, actor, note));
    }

    @PostMapping("/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String note,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra) {
        return execute(id, ud, ra, (page, actor) -> workflowService.rejectReview(id, actor, note));
    }

    @PostMapping("/request-publish")
    public String requestPublish(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails ud,
                                 RedirectAttributes ra) {
        return execute(id, ud, ra, (page, actor) -> workflowService.requestPublish(id, actor));
    }

    @PostMapping("/approve-publish")
    public String approvePublish(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails ud,
                                 RedirectAttributes ra) {
        return execute(id, ud, ra, (page, actor) -> workflowService.approvePublish(id, actor));
    }

    @PostMapping("/reject-publish")
    public String rejectPublish(@PathVariable Long id,
                                @RequestParam(required = false) String note,
                                @AuthenticationPrincipal UserDetails ud,
                                RedirectAttributes ra) {
        return execute(id, ud, ra, (page, actor) -> workflowService.rejectPublish(id, actor, note));
    }

    @PostMapping("/request-unpublish")
    public String requestUnpublish(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails ud,
                                   RedirectAttributes ra) {
        return execute(id, ud, ra, (page, actor) -> workflowService.requestUnpublish(id, actor));
    }

    @PostMapping("/confirm-unpublish")
    public String confirmUnpublish(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails ud,
                                   RedirectAttributes ra) {
        return execute(id, ud, ra, (page, actor) -> workflowService.confirmUnpublish(id, actor));
    }

    @PostMapping("/unpublish")
    public String unpublish(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails ud,
                            RedirectAttributes ra) {
        return execute(id, ud, ra, (page, actor) -> workflowService.unpublish(id, actor));
    }

    // ── 統一執行模板，處理例外並轉換為 flash 訊息 ──

    @FunctionalInterface
    private interface WorkflowAction {
        void run(Long pageId, CmsUser actor);
    }

    private String execute(Long pageId, UserDetails ud, RedirectAttributes ra, WorkflowAction action) {
        try {
            CmsUser actor = cmsUserService.loadUser(ud.getUsername());
            action.run(pageId, actor);
            ra.addFlashAttribute("success", "操作成功");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cms/pages";
    }
}
