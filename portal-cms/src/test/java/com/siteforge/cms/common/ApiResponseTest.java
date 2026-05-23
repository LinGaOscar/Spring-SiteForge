package com.siteforge.cms.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ok_setsSuccessAndData() {
        ApiResponse<String> res = ApiResponse.ok("hello");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isEqualTo("hello");
        assertThat(res.getError()).isNull();
    }

    @Test
    void ok_withNullData_isStillSuccess() {
        ApiResponse<Void> res = ApiResponse.ok(null);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isNull();
    }

    @Test
    void error_setsFailureAndErrorDetail() {
        ApiResponse<Void> res = ApiResponse.error("NOT_FOUND", "Resource not found");

        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getData()).isNull();
        assertThat(res.getError().getCode()).isEqualTo("NOT_FOUND");
        assertThat(res.getError().getMessage()).isEqualTo("Resource not found");
    }
}
