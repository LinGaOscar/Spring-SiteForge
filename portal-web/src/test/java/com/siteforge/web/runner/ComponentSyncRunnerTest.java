package com.siteforge.web.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siteforge.domain.entity.ComponentDefinition;
import com.siteforge.domain.repository.ComponentDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComponentSyncRunnerTest {

    @Mock ComponentDefinitionRepository componentDefinitionRepository;
    @Mock ResourcePatternResolver resourcePatternResolver;
    @InjectMocks ComponentSyncRunner componentSyncRunner;

    @BeforeEach
    void injectObjectMapper() throws Exception {
        var field = ComponentSyncRunner.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(componentSyncRunner, new ObjectMapper());
    }

    @Test
    void run_rwdFragment_parsesSchemaFromHtmlComment() throws Exception {
        String html = """
                <!DOCTYPE html>
                <html xmlns:th="http://www.thymeleaf.org">
                <body>
                <!--@component-schema
                {"deviceMode":"RWD","fields":[{"name":"logoText","type":"text","label":"Logo","default":"SF"}]}
                @end-component-schema-->
                <div th:fragment="header(config)"></div>
                </body>
                </html>
                """;
        Resource htmlResource = new ByteArrayResource(html.getBytes()) {
            @Override public String getFilename() { return "rwd_header.html"; }
        };
        when(resourcePatternResolver.getResources("classpath:templates/fragments/body/*.html"))
                .thenReturn(new Resource[0]);
        when(resourcePatternResolver.getResources("classpath:templates/fragments/header/*.html"))
                .thenReturn(new Resource[]{htmlResource});
        when(resourcePatternResolver.getResources("classpath:templates/fragments/footer/*.html"))
                .thenReturn(new Resource[0]);
        when(componentDefinitionRepository.findByTypeAndActiveTrue(anyString()))
                .thenReturn(Collections.emptyList());
        when(componentDefinitionRepository.findById("rwd_header"))
                .thenReturn(Optional.empty());
        when(componentDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        componentSyncRunner.run(null);

        ArgumentCaptor<ComponentDefinition> captor = ArgumentCaptor.forClass(ComponentDefinition.class);
        verify(componentDefinitionRepository, atLeastOnce()).save(captor.capture());
        ComponentDefinition saved = captor.getAllValues().stream()
                .filter(cd -> "rwd_header".equals(cd.getKey()))
                .findFirst().orElseThrow();
        assertThat(saved.getDeviceMode()).isEqualTo("RWD");
        assertThat(saved.getSchemaJson()).contains("logoText");
    }

    @Test
    void run_rwsFragment_parsesDeviceModeRws() throws Exception {
        String html = """
                <!DOCTYPE html>
                <html xmlns:th="http://www.thymeleaf.org">
                <body>
                <!--@component-schema
                {"deviceMode":"RWS","fields":[]}
                @end-component-schema-->
                <div th:fragment="body(config)"></div>
                </body>
                </html>
                """;
        Resource htmlResource = new ByteArrayResource(html.getBytes()) {
            @Override public String getFilename() { return "body_only_mobile.html"; }
        };
        when(resourcePatternResolver.getResources("classpath:templates/fragments/body/*.html"))
                .thenReturn(new Resource[]{htmlResource});
        when(resourcePatternResolver.getResources("classpath:templates/fragments/header/*.html"))
                .thenReturn(new Resource[0]);
        when(resourcePatternResolver.getResources("classpath:templates/fragments/footer/*.html"))
                .thenReturn(new Resource[0]);
        when(componentDefinitionRepository.findByTypeAndActiveTrue(anyString()))
                .thenReturn(Collections.emptyList());
        when(componentDefinitionRepository.findById("body_only_mobile"))
                .thenReturn(Optional.empty());
        when(componentDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        componentSyncRunner.run(null);

        ArgumentCaptor<ComponentDefinition> captor = ArgumentCaptor.forClass(ComponentDefinition.class);
        verify(componentDefinitionRepository, atLeastOnce()).save(captor.capture());
        ComponentDefinition saved = captor.getAllValues().stream()
                .filter(cd -> "body_only_mobile".equals(cd.getKey()))
                .findFirst().orElseThrow();
        assertThat(saved.getDeviceMode()).isEqualTo("RWS");
    }

    @Test
    void run_fragmentWithoutSchemaComment_defaultsToRwd() throws Exception {
        String html = """
                <!DOCTYPE html>
                <html xmlns:th="http://www.thymeleaf.org">
                <body>
                <div th:fragment="body(config)"><p>no schema comment</p></div>
                </body>
                </html>
                """;
        Resource htmlResource = new ByteArrayResource(html.getBytes()) {
            @Override public String getFilename() { return "rwd_body.html"; }
        };
        when(resourcePatternResolver.getResources("classpath:templates/fragments/body/*.html"))
                .thenReturn(new Resource[]{htmlResource});
        when(resourcePatternResolver.getResources("classpath:templates/fragments/header/*.html"))
                .thenReturn(new Resource[0]);
        when(resourcePatternResolver.getResources("classpath:templates/fragments/footer/*.html"))
                .thenReturn(new Resource[0]);
        when(componentDefinitionRepository.findByTypeAndActiveTrue(anyString()))
                .thenReturn(Collections.emptyList());
        when(componentDefinitionRepository.findById("rwd_body"))
                .thenReturn(Optional.empty());
        when(componentDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        componentSyncRunner.run(null);

        ArgumentCaptor<ComponentDefinition> captor = ArgumentCaptor.forClass(ComponentDefinition.class);
        verify(componentDefinitionRepository, atLeastOnce()).save(captor.capture());
        ComponentDefinition saved = captor.getAllValues().stream()
                .filter(cd -> "rwd_body".equals(cd.getKey()))
                .findFirst().orElseThrow();
        assertThat(saved.getDeviceMode()).isEqualTo("RWD");
        assertThat(saved.getSchemaJson()).isNull();
    }

    @Test
    void run_schemaWithNestedObjects_parsedCorrectly() throws Exception {
        String html = """
                <!DOCTYPE html>
                <html xmlns:th="http://www.thymeleaf.org">
                <body>
                <!--@component-schema
                {
                  "deviceMode": "RWD",
                  "fields": [
                    {"name": "navLinks", "type": "links", "label": "選單連結", "default": [
                      {"label": "首頁", "url": "/"},
                      {"label": "關於我們", "url": "/about"}
                    ]}
                  ]
                }
                @end-component-schema-->
                <div th:fragment="header(config)"></div>
                </body>
                </html>
                """;
        Resource htmlResource = new ByteArrayResource(html.getBytes()) {
            @Override public String getFilename() { return "rwd_header.html"; }
        };
        when(resourcePatternResolver.getResources("classpath:templates/fragments/body/*.html"))
                .thenReturn(new Resource[0]);
        when(resourcePatternResolver.getResources("classpath:templates/fragments/header/*.html"))
                .thenReturn(new Resource[]{htmlResource});
        when(resourcePatternResolver.getResources("classpath:templates/fragments/footer/*.html"))
                .thenReturn(new Resource[0]);
        when(componentDefinitionRepository.findByTypeAndActiveTrue(anyString()))
                .thenReturn(Collections.emptyList());
        when(componentDefinitionRepository.findById("rwd_header"))
                .thenReturn(Optional.empty());
        when(componentDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        componentSyncRunner.run(null);

        ArgumentCaptor<ComponentDefinition> captor = ArgumentCaptor.forClass(ComponentDefinition.class);
        verify(componentDefinitionRepository, atLeastOnce()).save(captor.capture());
        ComponentDefinition saved = captor.getAllValues().stream()
                .filter(cd -> "rwd_header".equals(cd.getKey()))
                .findFirst().orElseThrow();
        assertThat(saved.getDeviceMode()).isEqualTo("RWD");
        assertThat(saved.getSchemaJson()).contains("navLinks");
        assertThat(saved.getSchemaJson()).contains("關於我們");
    }

    @Test
    void run_richtextField_parsedInSchemaJson() throws Exception {
        String html = """
                <!DOCTYPE html>
                <html xmlns:th="http://www.thymeleaf.org">
                <body>
                <!--@component-schema
                {"deviceMode":"RWD","fields":[{"name":"content","type":"richtext","label":"內文","default":"<p>預設</p>"}]}
                @end-component-schema-->
                <div th:fragment="body(config)"></div>
                </body>
                </html>
                """;
        Resource res = new ByteArrayResource(html.getBytes()) {
            @Override public String getFilename() { return "rwd_richtext.html"; }
        };
        when(resourcePatternResolver.getResources("classpath:templates/fragments/body/*.html"))
                .thenReturn(new Resource[]{res});
        when(resourcePatternResolver.getResources("classpath:templates/fragments/header/*.html"))
                .thenReturn(new Resource[0]);
        when(resourcePatternResolver.getResources("classpath:templates/fragments/footer/*.html"))
                .thenReturn(new Resource[0]);
        when(componentDefinitionRepository.findByTypeAndActiveTrue(anyString()))
                .thenReturn(Collections.emptyList());
        when(componentDefinitionRepository.findById("rwd_richtext")).thenReturn(Optional.empty());
        when(componentDefinitionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        componentSyncRunner.run(null);

        ArgumentCaptor<ComponentDefinition> captor = ArgumentCaptor.forClass(ComponentDefinition.class);
        verify(componentDefinitionRepository, atLeastOnce()).save(captor.capture());
        ComponentDefinition saved = captor.getAllValues().stream()
                .filter(cd -> "rwd_richtext".equals(cd.getKey()))
                .findFirst().orElseThrow();
        assertThat(saved.getKey()).isEqualTo("rwd_richtext");
        assertThat(saved.getSchemaJson()).contains("\"type\":\"richtext\"");
        assertThat(saved.getSchemaJson()).contains("<p>預設</p>");
        assertThat(saved.getDeviceMode()).isEqualTo("RWD");
    }

    @Test
    void run_linksFieldWithArrayDefault_parsedCorrectly() throws Exception {
        String html = """
                <!DOCTYPE html>
                <html xmlns:th="http://www.thymeleaf.org">
                <body>
                <!--@component-schema
                {"deviceMode":"RWD","fields":[{"name":"links","type":"links","label":"連結","default":[{"label":"首頁","url":"/"}]}]}
                @end-component-schema-->
                <div th:fragment="body(config)"></div>
                </body>
                </html>
                """;
        Resource res = new ByteArrayResource(html.getBytes()) {
            @Override public String getFilename() { return "rwd_links_list.html"; }
        };
        when(resourcePatternResolver.getResources("classpath:templates/fragments/body/*.html"))
                .thenReturn(new Resource[]{res});
        when(resourcePatternResolver.getResources("classpath:templates/fragments/header/*.html"))
                .thenReturn(new Resource[0]);
        when(resourcePatternResolver.getResources("classpath:templates/fragments/footer/*.html"))
                .thenReturn(new Resource[0]);
        when(componentDefinitionRepository.findByTypeAndActiveTrue(anyString()))
                .thenReturn(Collections.emptyList());
        when(componentDefinitionRepository.findById("rwd_links_list")).thenReturn(Optional.empty());
        when(componentDefinitionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        componentSyncRunner.run(null);

        ArgumentCaptor<ComponentDefinition> captor = ArgumentCaptor.forClass(ComponentDefinition.class);
        verify(componentDefinitionRepository, atLeastOnce()).save(captor.capture());
        ComponentDefinition saved = captor.getAllValues().stream()
                .filter(cd -> "rwd_links_list".equals(cd.getKey()))
                .findFirst().orElseThrow();
        assertThat(saved.getKey()).isEqualTo("rwd_links_list");
        assertThat(saved.getSchemaJson()).contains("\"type\":\"links\"");
        assertThat(saved.getSchemaJson()).contains("\"url\":\"/\"");
    }
}
