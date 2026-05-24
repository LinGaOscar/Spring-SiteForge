package com.siteforge.web.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SwitchViewController {

    private static final int COOKIE_MAX_AGE = 30 * 24 * 60 * 60; // 30 天

    @GetMapping("/switch-view")
    public String switchView(@RequestParam String mode,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        String value = "desktop".equals(mode) ? "desktop" : "mobile";

        Cookie cookie = new Cookie("view_mode", value);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        // 切換後回到原頁，找不到 Referer 就回首頁
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}
