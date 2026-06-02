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
}
