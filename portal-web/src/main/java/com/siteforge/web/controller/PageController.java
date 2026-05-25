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

    // {*path} 捕捉任意深度路徑，例如 /about/team → path = "/about/team"
    @GetMapping("/{*path}")
    public String page(@PathVariable String path, HttpServletRequest request, Model model) {
        return renderPage(path, request, model);
    }

    private String renderPage(String path, HttpServletRequest request, Model model) {
        boolean isMobile = Boolean.TRUE.equals(request.getAttribute("isMobile"));

        // 路徑不存在或未發布 → 回首頁
        return pageRenderService.findPublishedPage(siteId, path)
                .map(page -> {
                    List<PageContentView> contents = pageRenderService.buildContentViews(page.getId());
                    // RWS 頁面（手機限定活動頁）不允許桌面裝置開啟
                    if (!isMobile && isRwsPage(page.getLayoutSet(), contents)) {
                        return "redirect:/mobile-required";
                    }
                    return buildModel(page, contents, model);
                })
                .orElse("redirect:/");
    }

    private String buildModel(Page page, List<PageContentView> contents, Model model) {
        model.addAttribute("page", page);
        model.addAttribute("layoutSet", page.getLayoutSet());
        model.addAttribute("pageContents", contents);
        model.addAttribute("headerTemplate", resolveKey(page.getLayoutSet(), "header"));
        model.addAttribute("footerTemplate", resolveKey(page.getLayoutSet(), "footer"));
        return "layout/base";
    }

    // RWS 頁面判定：header/footer key 或任一 body block key 以 RWS_ 開頭
    private boolean isRwsPage(LayoutSet layoutSet, List<PageContentView> contents) {
        if (layoutSet != null) {
            if (layoutSet.getHeaderKey() != null && layoutSet.getHeaderKey().name().startsWith("RWS_")) return true;
            if (layoutSet.getFooterKey() != null && layoutSet.getFooterKey().name().startsWith("RWS_")) return true;
        }
        return contents.stream().anyMatch(b -> b.blockKey().toUpperCase().startsWith("RWS_"));
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
