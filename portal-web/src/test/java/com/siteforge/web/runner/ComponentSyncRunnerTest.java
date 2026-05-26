package com.siteforge.web.runner;

import com.siteforge.domain.entity.ComponentDefinition;
import com.siteforge.domain.repository.ComponentDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ComponentSyncRunnerTest {

    @Mock ComponentDefinitionRepository componentDefinitionRepository;
    @Mock ResourcePatternResolver resourcePatternResolver;
    ComponentSyncRunner runner;

    @BeforeEach
    void setUp() {
        runner = new ComponentSyncRunner(componentDefinitionRepository, resourcePatternResolver);
    }

    @Test
    void run_newFragment_insertsActiveRecord() throws Exception {
        Resource mockRes = mock(Resource.class);
        when(mockRes.getFilename()).thenReturn("rwd_body_04.html");
        when(resourcePatternResolver.getResources("classpath:templates/fragments/body/*.html"))
                .thenReturn(new Resource[]{mockRes});
        when(componentDefinitionRepository.findByTypeAndActiveTrue("BODY")).thenReturn(List.of());
        when(componentDefinitionRepository.findById("rwd_body_04")).thenReturn(Optional.empty());

        runner.run(null);

        ArgumentCaptor<ComponentDefinition> captor = ArgumentCaptor.forClass(ComponentDefinition.class);
        verify(componentDefinitionRepository, times(1)).save(captor.capture());
        ComponentDefinition saved = captor.getValue();
        assertThat(saved.getKey()).isEqualTo("rwd_body_04");
        assertThat(saved.getType()).isEqualTo("BODY");
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getSyncedAt()).isNotNull();
    }

    @Test
    void run_removedFragment_marksInactive() throws Exception {
        when(resourcePatternResolver.getResources("classpath:templates/fragments/body/*.html"))
                .thenReturn(new Resource[0]);
        ComponentDefinition stale = new ComponentDefinition();
        stale.setKey("rwd_body_old");
        stale.setType("BODY");
        stale.setActive(true);
        when(componentDefinitionRepository.findByTypeAndActiveTrue("BODY")).thenReturn(List.of(stale));

        runner.run(null);

        ArgumentCaptor<ComponentDefinition> captor = ArgumentCaptor.forClass(ComponentDefinition.class);
        verify(componentDefinitionRepository).save(captor.capture());
        assertThat(captor.getValue().getActive()).isFalse();
    }
}
