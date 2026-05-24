package com.siteforge.cms.init;

import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.entity.Site;
import com.siteforge.domain.entity.Unit;
import com.siteforge.domain.enums.CmsUserRole;
import com.siteforge.domain.repository.CmsUserRepository;
import com.siteforge.domain.repository.SiteRepository;
import com.siteforge.domain.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final UnitRepository unitRepository;
    private final PasswordEncoder passwordEncoder;
    private final SiteRepository siteRepository;

    @Value("${cms.init.admin-username}")
    private String adminUsername;

    @Value("${cms.init.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            // 預設管理員帳號屬於 00100，同時擁有 OP + MA 方便開發測試完整流程
            Unit unit = unitRepository.findById("00100")
                    .orElseThrow(() -> new IllegalStateException("Unit 00100 not found, V5 migration may not have run"));

            CmsUser admin = new CmsUser();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setEnabled(true);
            admin.setUnit(unit);
            admin.setRoles(Set.of(CmsUserRole.OP, CmsUserRole.MA));
            userRepository.save(admin);

            log.info("=== Dev seed: {} / [configured in application-dev.yml] unit=00100 roles=[OP,MA] ===", adminUsername);
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
