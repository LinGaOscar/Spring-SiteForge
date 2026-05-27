package com.siteforge.web.controller;

import com.siteforge.domain.repository.ComponentDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreviewControllerTest {

    @Mock ComponentDefinitionRepository componentDefinitionRepository;
    @InjectMocks PreviewController previewController;

    @Test
    void previewComponent_validKey_returnsComponentView() {
        when(componentDefinitionRepository.existsByKeyAndTypeAndActiveTrue("rwd_body_01", "BODY"))
            .thenReturn(true);
        Model model = mock(Model.class);

        String view = previewController.previewComponent("rwd_body_01", model);

        assertThat(view).isEqualTo("preview/component");
    }

    @Test
    void previewComponent_unknownKey_throwsNotFound() {
        when(componentDefinitionRepository.existsByKeyAndTypeAndActiveTrue("bad_key", "BODY"))
            .thenReturn(false);
        Model model = mock(Model.class);

        assertThatThrownBy(() -> previewController.previewComponent("bad_key", model))
            .isInstanceOf(ResponseStatusException.class);
    }
}
