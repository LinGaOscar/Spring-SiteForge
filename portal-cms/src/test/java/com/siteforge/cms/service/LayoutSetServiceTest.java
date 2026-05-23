package com.siteforge.cms.service;

import com.siteforge.cms.dto.LayoutSetRequest;
import com.siteforge.cms.dto.LayoutSetResponse;
import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.enums.TemplateKey;
import com.siteforge.domain.repository.LayoutSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LayoutSetServiceTest {

    @Mock LayoutSetRepository layoutSetRepository;
    @InjectMocks LayoutSetService layoutSetService;

    @Test
    void create_validRequest_returnsSavedResponse() {
        LayoutSetRequest request = new LayoutSetRequest();
        request.setName("Default Layout");
        request.setHeaderKey(TemplateKey.HEADER_DEFAULT);
        request.setBodyKey(TemplateKey.BODY_STANDARD);
        request.setFooterKey(TemplateKey.FOOTER_DEFAULT);

        LayoutSet saved = new LayoutSet();
        saved.setId(1L);
        saved.setName("Default Layout");
        saved.setHeaderKey(TemplateKey.HEADER_DEFAULT);
        saved.setBodyKey(TemplateKey.BODY_STANDARD);
        saved.setFooterKey(TemplateKey.FOOTER_DEFAULT);
        saved.setEnabled(true);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        when(layoutSetRepository.save(any())).thenReturn(saved);

        LayoutSetResponse response = layoutSetService.create(request, "manager");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Default Layout");
    }

    @Test
    void create_wrongHeaderKey_throwsIllegalArgument() {
        LayoutSetRequest request = new LayoutSetRequest();
        request.setName("Bad Layout");
        request.setHeaderKey(TemplateKey.BODY_STANDARD); // Wrong: BODY key used for header slot
        request.setBodyKey(TemplateKey.BODY_STANDARD);
        request.setFooterKey(TemplateKey.FOOTER_DEFAULT);

        assertThatThrownBy(() -> layoutSetService.create(request, "manager"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("headerKey must be a HEADER_");
    }

    @Test
    void update_notFound_throwsIllegalArgument() {
        when(layoutSetRepository.findById(99L)).thenReturn(Optional.empty());
        LayoutSetRequest request = new LayoutSetRequest();
        request.setName("X");
        request.setHeaderKey(TemplateKey.HEADER_DEFAULT);
        request.setBodyKey(TemplateKey.BODY_STANDARD);
        request.setFooterKey(TemplateKey.FOOTER_DEFAULT);

        assertThatThrownBy(() -> layoutSetService.update(99L, request, "manager"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("LayoutSet not found");
    }
}
