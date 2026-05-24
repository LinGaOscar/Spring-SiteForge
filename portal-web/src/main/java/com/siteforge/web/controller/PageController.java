package com.siteforge.web.controller;

import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.enums.TemplateKey;
import com.siteforge.web.service.PageContentView;
import com.siteforge.web.service.PageRenderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final PageRenderService pageRenderService;

    @Value("${portal.site-id}")
    private Long siteId;

    @GetMapping("/")
    public String index(HttpServletRequest request, Model model) {
        return renderPage("/", request, model);
    }

    @GetMapping("/{path}")
    public String page(@PathVariable String path, HttpServletRequest request, Model model) {
        return renderPage("/" + path, request, model);
    }

    private String renderPage(String path, HttpServletRequest request, Model model) {
        boolean isMobile = Boolean.TRUE.equals(request.getAttribute("isMobile"));

        return pageRenderService.findPublishedPage(siteId, path)
                .map(page -> buildModel(page, isMobile, model))
                .orElse("error/404");
    }

    private String buildModel(Page page, boolean isMobile, Model model) {
        List<PageContentView> contents = pageRenderService.buildContentViews(page.getId());

        model.addAttribute("page", page);
        model.addAttribute("layoutSet", page.getLayoutSet());
        model.addAttribute("pageContents", contents);
        model.addAttribute("headerTemplate", resolveKey(page.getLayoutSet(), "header", isMobile));
        model.addAttribute("bodyTemplate",   resolveKey(page.getLayoutSet(), "body",   isMobile));
        model.addAttribute("footerTemplate", resolveKey(page.getLayoutSet(), "footer", isMobile));

        return "layout/base";
    }

    /**
     * 根據裝置類型決定最終模板名稱。
     * 手機版強制將 rwd_ 前綴替換為 rws_，讓同一個 layoutSet 設定可兼顧兩種裝置。
     */
    private String resolveKey(LayoutSet layoutSet, String part, boolean isMobile) {
        TemplateKey key = switch (part) {
            case "header" -> layoutSet != null ? layoutSet.getHeaderKey() : TemplateKey.RWD_HEADER;
            case "body"   -> layoutSet != null ? layoutSet.getBodyKey()   : TemplateKey.RWD_BODY;
            case "footer" -> layoutSet != null ? layoutSet.getFooterKey() : TemplateKey.RWD_FOOTER;
            default -> throw new IllegalArgumentException("Unknown part: " + part);
        };

        String templateName = key.name().toLowerCase(); // e.g. "rwd_header"

        // body 模板不區分裝置（Bootstrap responsive 已處理），header/footer 才需要切換
        if (isMobile && !part.equals("body")) {
            templateName = templateName.replace("rwd_", "rws_");
        }

        return templateName;
    }
}
