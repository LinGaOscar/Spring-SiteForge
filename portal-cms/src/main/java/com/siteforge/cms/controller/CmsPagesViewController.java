package com.siteforge.cms.controller;

import com.siteforge.cms.service.CmsUserService;
import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.enums.CmsUserRole;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.enums.TemplateKey;
import com.siteforge.domain.repository.LayoutSetRepository;
import com.siteforge.domain.repository.PageContentRepository;
import com.siteforge.domain.repository.PageRepository;
import com.siteforge.domain.repository.ComponentDefinitionRepository;
import com.siteforge.domain.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/cms/pages")
@RequiredArgsConstructor
public class CmsPagesViewController {

    private final CmsUserService cmsUserService;
    private final PageRepository pageRepository;
    private final LayoutSetRepository layoutSetRepository;
    private final PageContentRepository pageContentRepository;
    private final SiteRepository siteRepository;
    private final ComponentDefinitionRepository componentDefinitionRepository;

    @Value("${cms.preview-base-url:}")
    private String previewBaseUrl;

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @AuthenticationPrincipal UserDetails ud,
                       Model model) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        String unitCode = cmsUserService.unitCode(actor);

        List<Page> pages = (status != null && !status.isBlank())
                ? pageRepository.findByUnitCodeAndStatus(unitCode, PageStatus.valueOf(status))
                : pageRepository.findByUnitCode(unitCode);

        model.addAttribute("actor", actor);
        model.addAttribute("pages", pages);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", PageStatus.values());
        model.addAttribute("isMA", actor.getRoles().contains(CmsUserRole.MA));
        model.addAttribute("isOP", actor.getRoles().contains(CmsUserRole.OP));
        model.addAttribute("previewBaseUrl", previewBaseUrl);
        return "cms/pages/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal UserDetails ud, Model model) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        if (!actor.getRoles().contains(CmsUserRole.OP)) {
            return "redirect:/cms/pages";
        }
        model.addAttribute("actor", actor);
        model.addAttribute("page", new Page());
        model.addAttribute("sites", siteRepository.findAll());
        model.addAttribute("headerKeys", TemplateKey.headers());
        model.addAttribute("footerKeys", TemplateKey.footers());
        return "cms/pages/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails ud,
                           Model model) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Page not found: " + id));

        boolean editable = actor.getRoles().contains(CmsUserRole.OP)
                && (page.getStatus() == PageStatus.DRAFT || page.getStatus() == PageStatus.APPROVED)
                && unitCode(actor).equals(unitCode(page));
        if (!editable) return "redirect:/cms/pages";

        model.addAttribute("actor", actor);
        model.addAttribute("page", page);
        model.addAttribute("headerKeys", TemplateKey.headers());
        model.addAttribute("footerKeys", TemplateKey.footers());
        model.addAttribute("bodyKeys", componentDefinitionRepository.findByTypeAndActiveTrue("BODY"));
        model.addAttribute("pageContents", pageContentRepository.findByPageIdOrderBySortOrder(id));
        model.addAttribute("previewBaseUrl", previewBaseUrl);
        return "cms/pages/form";
    }

    @PostMapping
    public String create(@RequestParam String path,
                         @RequestParam String title,
                         @RequestParam(required = false) String seoTitle,
                         @RequestParam(required = false) String seoDescription,
                         @RequestParam Long siteId,
                         @RequestParam(required = false) String headerKey,
                         @RequestParam(required = false) String footerKey,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        if (!actor.getRoles().contains(CmsUserRole.OP)) {
            ra.addFlashAttribute("error", "無新增頁面權限");
            return "redirect:/cms/pages";
        }
        var site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Site not found"));

        Page page = new Page();
        page.setSite(site);
        page.setPath(path);
        page.setTitle(title);
        page.setSeoTitle(seoTitle);
        page.setSeoDescription(seoDescription);
        page.setLayoutSet(resolveLayoutSet(headerKey, footerKey, actor.getUsername()));
        page.setUnit(actor.getUnit());
        page.setStatus(PageStatus.DRAFT);
        page.setCreatedBy(actor.getUsername());
        page.setUpdatedBy(actor.getUsername());
        Page saved = pageRepository.save(page);

        ra.addFlashAttribute("success", "頁面已建立（草稿），請繼續設定 Body 區塊");
        return "redirect:/cms/pages/" + saved.getId() + "/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String path,
                         @RequestParam String title,
                         @RequestParam(required = false) String seoTitle,
                         @RequestParam(required = false) String seoDescription,
                         @RequestParam(required = false) String headerKey,
                         @RequestParam(required = false) String footerKey,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Page not found: " + id));

        boolean editable = actor.getRoles().contains(CmsUserRole.OP)
                && (page.getStatus() == PageStatus.DRAFT || page.getStatus() == PageStatus.APPROVED)
                && unitCode(actor).equals(unitCode(page));
        if (!editable) {
            ra.addFlashAttribute("error", "無編輯此頁面的權限");
            return "redirect:/cms/pages";
        }

        page.setPath(path);
        page.setTitle(title);
        page.setSeoTitle(seoTitle);
        page.setSeoDescription(seoDescription);
        page.setLayoutSet(resolveLayoutSet(headerKey, footerKey, actor.getUsername()));
        page.setUpdatedBy(actor.getUsername());
        pageRepository.save(page);

        ra.addFlashAttribute("success", "頁面已更新");
        return "redirect:/cms/pages/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Page not found: " + id));

        boolean deletable = actor.getRoles().contains(CmsUserRole.OP)
                && page.getStatus() == PageStatus.DRAFT
                && unitCode(actor).equals(unitCode(page));
        if (!deletable) {
            ra.addFlashAttribute("error", "僅可刪除草稿狀態的頁面");
            return "redirect:/cms/pages";
        }

        pageRepository.deleteById(id);
        ra.addFlashAttribute("success", "頁面已刪除");
        return "redirect:/cms/pages";
    }

    // 依 headerKey + footerKey 找現有 LayoutSet，找不到則自動建立
    private LayoutSet resolveLayoutSet(String headerKeyStr, String footerKeyStr, String username) {
        if (headerKeyStr == null || headerKeyStr.isBlank()
                || footerKeyStr == null || footerKeyStr.isBlank()) return null;
        TemplateKey hk = TemplateKey.valueOf(headerKeyStr);
        TemplateKey fk = TemplateKey.valueOf(footerKeyStr);
        return layoutSetRepository.findFirstByHeaderKeyAndFooterKey(hk, fk)
                .orElseGet(() -> {
                    LayoutSet ls = new LayoutSet();
                    ls.setHeaderKey(hk);
                    ls.setFooterKey(fk);
                    ls.setName(hk.name() + " + " + fk.name());
                    ls.setCreatedBy(username);
                    ls.setUpdatedBy(username);
                    return layoutSetRepository.save(ls);
                });
    }

    private String unitCode(CmsUser u) {
        return u.getUnit() != null ? u.getUnit().getCode() : "";
    }

    private String unitCode(Page p) {
        return p.getUnit() != null ? p.getUnit().getCode() : "";
    }
}
