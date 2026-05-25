package com.siteforge.cms.security;

import com.siteforge.cms.controller.PageController;
import com.siteforge.cms.service.PageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 驗證 @EnableMethodSecurity 生效：
 * - EDITOR 角色呼叫 DELETE /api/cms/pages/{id} → 403
 * - MANAGER 角色呼叫 DELETE /api/cms/pages/{id} → 204
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityConfigMethodSecurityTest.TestConfig.class)
@WebAppConfiguration
class SecurityConfigMethodSecurityTest {

    @Autowired
    WebApplicationContext wac;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void deleteAsEditor_returns403() throws Exception {
        mockMvc.perform(delete("/api/cms/pages/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MA")
    void deleteAsManager_returns204() throws Exception {
        mockMvc.perform(delete("/api/cms/pages/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    // 精簡的 test context：
    // @Import(SecurityConfig.class) 帶入 @EnableWebSecurity + @EnableMethodSecurity + SecurityFilterChain
    @Configuration
    @org.springframework.web.servlet.config.annotation.EnableWebMvc
    @org.springframework.context.annotation.Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        PageService pageService() {
            return mock(PageService.class);
        }

        @Bean
        PageController pageController(PageService pageService) {
            return new PageController(pageService);
        }

        // SecurityConfig 需要 UserDetailsService；提供 mock 避免拉入 JPA
        @Bean
        UserDetailsService userDetailsService() {
            return mock(UserDetailsService.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
