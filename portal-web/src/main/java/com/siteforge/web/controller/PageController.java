package com.siteforge.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.entity.Page;
import com.siteforge.domain.enums.TemplateKey;
import com.siteforge.web.service.PageContentView;
import com.siteforge.web.service.PageRenderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PageController {

    private final PageRenderService pageRenderService;
    private final ObjectMapper objectMapper;

    @Value("${portal.site-id}")
    private Long siteId;

    @GetMapping("/")
    public String index(HttpServletRequest request, HttpServletResponse response, Model model) {
        return renderPage("/", request, response, model);
    }

    @GetMapping("/{*path}")
    public String page(@PathVariable String path, HttpServletRequest request,
                       HttpServletResponse response, Model model) {
        return renderPage(path, request, response, model);
    }

    private String renderPage(String path, HttpServletRequest request,
                              HttpServletResponse response, Model model) {
        boolean isMobile = Boolean.TRUE.equals(request.getAttribute("isMobile"));
        return pageRenderService.findPublishedPage(siteId, path)
                .map(page -> {
                    List<PageContentView> contents = pageRenderService.buildContentViews(page.getId());
                    if (!isMobile && isRwsPage(page.getLayoutSet(), contents)) {
                        return "redirect:/mobile-required";
                    }
                    return buildModel(page, contents, model);
                })
                .orElseGet(() -> {
                    // 找不到發布頁面時回傳 503，避免 redirect:/ 造成無限迴圈
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    return "error/404";
                });
    }

    private String buildModel(Page page, List<PageContentView> contents, Model model) {
        model.addAttribute("page", page);
        model.addAttribute("layoutSet", page.getLayoutSet());
        model.addAttribute("pageContents", contents);
        model.addAttribute("headerTemplate", resolveKey(page.getLayoutSet(), "header"));
        model.addAttribute("footerTemplate", resolveKey(page.getLayoutSet(), "footer"));
        model.addAttribute("headerConfig", parseConfig(page.getHeaderConfigJson()));
        model.addAttribute("footerConfig", parseConfig(page.getFooterConfigJson()));
        return "layout/base";
    }

    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("無法解析 config JSON: {}", e.getMessage());
            return Collections.emptyMap();
        }
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
