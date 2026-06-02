package com.siteforge.cms.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerServiceTest {

    private final HtmlSanitizerService sanitizer = new HtmlSanitizerService();

    @Test
    void sanitize_scriptTag_removed() {
        String result = sanitizer.sanitize("<script>alert('xss')</script>Hello");
        assertThat(result).doesNotContain("<script>");
        assertThat(result).contains("Hello");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    void sanitize_boldTag_preserved() {
        String result = sanitizer.sanitize("<b>Hello</b>");
        assertThat(result).contains("<b>Hello</b>");
    }

    @Test
    void sanitize_null_returnsNull() {
        assertThat(sanitizer.sanitize(null)).isNull();
    }

    @Test
    void sanitize_blank_returnsBlank() {
        String input = "  ";
        assertThat(sanitizer.sanitize(input)).isEqualTo(input);
    }

    @Test
    void sanitize_onerrorAttribute_removed() {
        String result = sanitizer.sanitize("<img src=\"x\" onerror=\"alert(1)\">");
        assertThat(result).doesNotContain("onerror");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    void sanitize_javascriptHref_removed() {
        String result = sanitizer.sanitize("<a href=\"javascript:alert(1)\">click</a>");
        assertThat(result).doesNotContain("javascript:");
    }

    @Test
    void sanitize_onclickAttribute_removed() {
        String result = sanitizer.sanitize("<div onclick=\"steal()\">hover</div>");
        assertThat(result).doesNotContain("onclick");
        assertThat(result).doesNotContain("steal");
    }
}
