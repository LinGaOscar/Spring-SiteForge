package com.siteforge.web.controller;

import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.enums.TemplateKey;
import com.siteforge.domain.repository.PageRepository;
import com.siteforge.web.service.PageContentView;
import com.siteforge.web.service.PageRenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** 僅 dev profile 啟用：CMS 預覽用，跳過 PUBLISHED 狀態檢查 */
@Profile("dev")
@Controller
@RequiredArgsConstructor
public class PreviewController {

    private final PageRepository pageRepository;
    private final PageRenderService pageRenderService;

    @GetMapping("/preview/{pageId}")
    public String preview(@PathVariable Long pageId, Model model) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found: " + pageId));

        List<PageContentView> contents = pageRenderService.buildContentViews(pageId);
        model.addAttribute("page", page);
        model.addAttribute("layoutSet", page.getLayoutSet());
        model.addAttribute("pageContents", contents);
        model.addAttribute("headerTemplate", resolveKey(page.getLayoutSet(), "header"));
        model.addAttribute("footerTemplate", resolveKey(page.getLayoutSet(), "footer"));
        model.addAttribute("isPreview", true);
        return "layout/base";
    }

    private String resolveKey(LayoutSet layoutSet, String part) {
        TemplateKey key = switch (part) {
            case "header" -> layoutSet != null ? layoutSet.getHeaderKey() : TemplateKey.RWD_HEADER;
            case "footer" -> layoutSet != null ? layoutSet.getFooterKey() : TemplateKey.RWD_FOOTER;
            default -> throw new IllegalArgumentException("Unknown part: " + part);
        };
        return key.name().toLowerCase();
    }
}
