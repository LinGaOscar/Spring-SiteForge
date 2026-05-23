package com.siteforge.domain.repository;

import com.siteforge.domain.entity.Site;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SiteRepositoryTest {

    @Autowired
    private SiteRepository siteRepository;

    @Test
    void saveAndFindByCode() {
        Site site = new Site();
        site.setCode("default");
        site.setName("SpringSiteForge");
        siteRepository.save(site);

        assertThat(siteRepository.findByCode("default")).isPresent()
            .hasValueSatisfying(s -> assertThat(s.getCode()).isEqualTo("default"));
    }

    @Test
    void findByEnabledTrue_returnsOnlyEnabledSites() {
        Site a = new Site(); a.setCode("a"); a.setName("A"); a.setEnabled(true);
        Site b = new Site(); b.setCode("b"); b.setName("B"); b.setEnabled(false);
        siteRepository.save(a);
        siteRepository.save(b);

        assertThat(siteRepository.findByEnabledTrue()).hasSize(1)
            .first().satisfies(s -> assertThat(s.getCode()).isEqualTo("a"));
    }
}
