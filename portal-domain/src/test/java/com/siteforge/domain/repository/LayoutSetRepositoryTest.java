package com.siteforge.domain.repository;

import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.enums.TemplateKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LayoutSetRepositoryTest {

    @Autowired
    private LayoutSetRepository layoutSetRepository;

    @Test
    void saveAndFindEnabledLayouts() {
        LayoutSet enabled = new LayoutSet();
        enabled.setName("Default Layout");
        enabled.setHeaderKey(TemplateKey.HEADER_DEFAULT);
        enabled.setBodyKey(TemplateKey.BODY_STANDARD);
        enabled.setFooterKey(TemplateKey.FOOTER_DEFAULT);
        enabled.setEnabled(true);
        layoutSetRepository.save(enabled);

        LayoutSet disabled = new LayoutSet();
        disabled.setName("Disabled Layout");
        disabled.setHeaderKey(TemplateKey.HEADER_DEFAULT);
        disabled.setBodyKey(TemplateKey.BODY_LANDING);
        disabled.setFooterKey(TemplateKey.FOOTER_DEFAULT);
        disabled.setEnabled(false);
        layoutSetRepository.save(disabled);

        assertThat(layoutSetRepository.findByEnabledTrue()).hasSize(1)
            .first().satisfies(l -> assertThat(l.getName()).isEqualTo("Default Layout"));
    }
}
