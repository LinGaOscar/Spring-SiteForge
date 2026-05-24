package com.siteforge.web.interceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.regex.Pattern;

public class DeviceInterceptor implements HandlerInterceptor {

    private static final String ATTR_IS_MOBILE = "isMobile";
    private static final String COOKIE_VIEW_MODE = "view_mode";

    // 常見手機 UA 關鍵字（平板不在手機版範圍內）
    private static final Pattern MOBILE_UA = Pattern.compile(
            "(?i)(mobile|android(?!.*tablet)|iphone|ipod|blackberry|windows phone)");

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        boolean isMobile = detectDevice(request);
        request.setAttribute(ATTR_IS_MOBILE, isMobile);
        return true;
    }

    private boolean detectDevice(HttpServletRequest request) {
        // Cookie 優先：使用者手動切換的偏好永遠蓋過 UA 偵測
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> COOKIE_VIEW_MODE.equals(c.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .map(v -> "mobile".equals(v))
                    .orElseGet(() -> isMobileUserAgent(request));
        }
        return isMobileUserAgent(request);
    }

    private boolean isMobileUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null && MOBILE_UA.matcher(ua).find();
    }
}
