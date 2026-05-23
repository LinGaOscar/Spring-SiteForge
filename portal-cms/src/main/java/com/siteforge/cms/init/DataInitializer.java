package com.siteforge.cms.init;

import com.siteforge.domain.entity.CmsRole;
import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.entity.Site;
import com.siteforge.domain.repository.CmsRoleRepository;
import com.siteforge.domain.repository.CmsUserRepository;
import com.siteforge.domain.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CmsUserRepository userRepository;
    private final CmsRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SiteRepository siteRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            CmsRole managerRole = new CmsRole();
            managerRole.setName("ROLE_MANAGER");
            roleRepository.save(managerRole);

            CmsRole editorRole = new CmsRole();
            editorRole.setName("ROLE_EDITOR");
            roleRepository.save(editorRole);

            CmsUser manager = new CmsUser();
            manager.setUsername("manager");
            manager.setPassword(passwordEncoder.encode("siteforge2026"));
            manager.setEnabled(true);
            manager.setRoles(Set.of(managerRole));
            userRepository.save(manager);

            log.info("=== Dev seed: manager / siteforge2026 ===");
        }

        if (siteRepository.count() == 0) {
            Site site = new Site();
            site.setCode("default");
            site.setName("SpringSiteForge");
            site.setDomain("localhost");
            siteRepository.save(site);
            log.info("=== Dev seed: site default ===");
        }
    }
}
