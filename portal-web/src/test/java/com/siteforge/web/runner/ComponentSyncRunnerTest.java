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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComponentSyncRunnerTest {

    @Mock ComponentDefinitionRepository componentDefinitionRepository;
    @Mock ResourcePatternResolver resourcePatternResolver;
    @InjectMocks ComponentSyncRunner componentSyncRunner;

    @BeforeEach
    void injectObjectMapper() throws Exception {
        Field field = ComponentSyncRunner.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(componentSyncRunner, new ObjectMapper());
    }

    @Test
    void run_bodyFragment_savedWithDeviceModeFromSchema() throws Exception {
        Resource htmlResource = mock(Resource.class);
        when(htmlResource.getFilename()).thenReturn("rwd_body_01.html");
        when(resourcePatternResolver.getResources("classpath:templates/fragments/body/*.html"))
                .thenReturn(new Resource[]{htmlResource});
        when(resourcePatternResolver.getResources("classpath:templates/fragments/header/*.html"))
                .thenReturn(new Resource[0]);
        when(resourcePatternResolver.getResources("classpath:templates/fragments/footer/*.html"))
                .thenReturn(new Resource[0]);

        String schema = "{\"deviceMode\":\"RWD\",\"fields\":[]}";
        Resource schemaResource = new ByteArrayResource(schema.getBytes()) {
            @Override public boolean exists() { return true; }
        };
        when(resourcePatternResolver.getResource("classpath:component-schemas/rwd_body_01.json"))
                .thenReturn(schemaResource);

        when(componentDefinitionRepository.findByTypeAndActiveTrue(anyString()))
                .thenReturn(Collections.emptyList());
        when(componentDefinitionRepository.findById("rwd_body_01"))
                .thenReturn(Optional.empty());
        when(componentDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        componentSyncRunner.run(null);

        ArgumentCaptor<ComponentDefinition> captor = ArgumentCaptor.forClass(ComponentDefinition.class);
        verify(componentDefinitionRepository, atLeastOnce()).save(captor.capture());

        ComponentDefinition saved = captor.getAllValues().stream()
                .filter(cd -> "rwd_body_01".equals(cd.getKey()))
                .findFirst().orElseThrow();
        assertThat(saved.getDeviceMode()).isEqualTo("RWD");
        assertThat(saved.getSchemaJson()).contains("deviceMode");
        assertThat(saved.getType()).isEqualTo("BODY");
    }

    @Test
    void run_rwsFragment_hasRwsDeviceMode() throws Exception {
        Resource htmlResource = mock(Resource.class);
        when(htmlResource.getFilename()).thenReturn("rws_body.html");
        when(resourcePatternResolver.getResources("classpath:templates/fragments/body/*.html"))
                .thenReturn(new Resource[]{htmlResource});
        when(resourcePatternResolver.getResources("classpath:templates/fragments/header/*.html"))
                .thenReturn(new Resource[0]);
        when(resourcePatternResolver.getResources("classpath:templates/fragments/footer/*.html"))
                .thenReturn(new Resource[0]);

        String schema = "{\"deviceMode\":\"RWS\",\"fields\":[]}";
        Resource schemaResource = new ByteArrayResource(schema.getBytes()) {
            @Override public boolean exists() { return true; }
        };
        when(resourcePatternResolver.getResource("classpath:component-schemas/rws_body.json"))
                .thenReturn(schemaResource);

        when(componentDefinitionRepository.findByTypeAndActiveTrue(anyString()))
                .thenReturn(Collections.emptyList());
        when(componentDefinitionRepository.findById("rws_body"))
                .thenReturn(Optional.empty());
        when(componentDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        componentSyncRunner.run(null);

        ArgumentCaptor<ComponentDefinition> captor = ArgumentCaptor.forClass(ComponentDefinition.class);
        verify(componentDefinitionRepository, atLeastOnce()).save(captor.capture());

        ComponentDefinition saved = captor.getAllValues().stream()
                .filter(cd -> "rws_body".equals(cd.getKey()))
                .findFirst().orElseThrow();
        assertThat(saved.getDeviceMode()).isEqualTo("RWS");
    }
}
