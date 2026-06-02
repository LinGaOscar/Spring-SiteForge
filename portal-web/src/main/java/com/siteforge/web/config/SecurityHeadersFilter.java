package com.siteforge.web.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    // 前台引用 jsdelivr（Bootstrap）與 unpkg（Vue 3）
    // img-src 需包含 CMS 來源，因為 rwd_text_image 等元件的 imageUrl 指向 CMS uploads
    private final String csp;

    public SecurityHeadersFilter(@Value("${cms.asset-base-url:}") String cmsAssetBaseUrl) {
        String imgSrc = "img-src 'self' data: blob:" + (cmsAssetBaseUrl.isBlank() ? "" : " " + cmsAssetBaseUrl);
        this.csp =
            "default-src 'self'; " +
            "script-src 'self' https://cdn.jsdelivr.net https://unpkg.com; " +
            "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
            imgSrc + "; " +
            "font-src 'self' data:; " +
            "connect-src 'self'; " +
            "form-action 'self'; " +
            "frame-ancestors 'none'";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Content-Security-Policy", csp);
        filterChain.doFilter(request, response);
    }
}
