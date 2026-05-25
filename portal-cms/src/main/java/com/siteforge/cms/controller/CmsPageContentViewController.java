package com.siteforge.cms.controller;

import com.siteforge.cms.dto.PageContentRequest;
import com.siteforge.cms.service.CmsUserService;
import com.siteforge.cms.service.PageContentService;
import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.enums.CmsUserRole;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.repository.PageContentRepository;
import com.siteforge.domain.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cms/pages/{pageId}/content")
@RequiredArgsConstructor
public class CmsPageContentViewController {

    private final CmsUserService cmsUserService;
    private final PageContentService pageContentService;
    private final PageRepository pageRepository;
    private final PageContentRepository pageContentRepository;

    @GetMapping
    public String list(@PathVariable Long pageId,
                       @AuthenticationPrincipal UserDetails ud,
                       Model model) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        Page page = loadAndCheck(pageId, actor);

        model.addAttribute("actor", actor);
        model.addAttribute("page", page);
        model.addAttribute("contents", pageContentRepository.findByPageIdOrderBySortOrder(pageId));
        return "cms/pages/content";
    }

    @PostMapping
    public String create(@PathVariable Long pageId,
                         @RequestParam String blockKey,
                         @RequestParam int sortOrder,
                         @RequestParam String contentJson,
                         @RequestParam(defaultValue = "zh-TW") String locale,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        try {
            loadAndCheck(pageId, actor);
            PageContentRequest req = buildRequest(blockKey, sortOrder, contentJson, locale);
            pageContentService.create(pageId, req);
            ra.addFlashAttribute("success", "區塊已新增");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cms/pages/" + pageId + "/content";
    }

    @PostMapping("/{contentId}")
    public String update(@PathVariable Long pageId,
                         @PathVariable Long contentId,
                         @RequestParam String blockKey,
                         @RequestParam int sortOrder,
                         @RequestParam String contentJson,
                         @RequestParam(defaultValue = "zh-TW") String locale,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        try {
            loadAndCheck(pageId, actor);
            PageContentRequest req = buildRequest(blockKey, sortOrder, contentJson, locale);
            pageContentService.update(pageId, contentId, req);
            ra.addFlashAttribute("success", "區塊已更新");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cms/pages/" + pageId + "/content";
    }

    @PostMapping("/{contentId}/delete")
    public String delete(@PathVariable Long pageId,
                         @PathVariable Long contentId,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        try {
            loadAndCheck(pageId, actor);
            pageContentService.delete(pageId, contentId);
            ra.addFlashAttribute("success", "區塊已刪除");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cms/pages/" + pageId + "/content";
    }

    private Page loadAndCheck(Long pageId, CmsUser actor) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Page not found: " + pageId));
        boolean ok = actor.getRoles().contains(CmsUserRole.OP)
                && (page.getStatus() == PageStatus.DRAFT || page.getStatus() == PageStatus.APPROVED)
                && unitCode(actor).equals(unitCode(page));
        if (!ok)
            throw new IllegalStateException("無權限編輯此頁面的內容區塊");
        return page;
    }

    private PageContentRequest buildRequest(String blockKey, int sortOrder, String contentJson, String locale) {
        PageContentRequest req = new PageContentRequest();
        req.setBlockKey(blockKey);
        req.setSortOrder(sortOrder);
        req.setContentJson(contentJson);
        req.setLocale(locale);
        return req;
    }

    private String unitCode(CmsUser u) {
        return u.getUnit() != null ? u.getUnit().getCode() : "";
    }

    private String unitCode(Page p) {
        return p.getUnit() != null ? p.getUnit().getCode() : "";
    }
}
