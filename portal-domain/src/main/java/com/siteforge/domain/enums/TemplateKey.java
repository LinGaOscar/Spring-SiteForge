package com.siteforge.domain.enums;

import java.util.Arrays;
import java.util.List;

public enum TemplateKey {
    // 通用備用模板
    RWD_HEADER,
    RWS_HEADER,
    RWD_FOOTER,
    RWS_FOOTER,
    RWD_BODY,
    RWS_BODY,
    // 編號版型（實際使用）
    RWD_HEADER_01,
    RWD_FOOTER_01,
    RWD_BODY_01,
    RWD_BODY_02,
    RWD_BODY_03;

    public static List<TemplateKey> headers() {
        return Arrays.stream(values()).filter(k -> k.name().contains("HEADER")).toList();
    }

    public static List<TemplateKey> footers() {
        return Arrays.stream(values()).filter(k -> k.name().contains("FOOTER")).toList();
    }

    public static List<TemplateKey> bodies() {
        return Arrays.stream(values()).filter(k -> k.name().contains("BODY")).toList();
    }
}
