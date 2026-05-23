package com.siteforge.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.siteforge.domain.entity")
@EnableJpaRepositories("com.siteforge.domain.repository")
public class PortalCmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PortalCmsApplication.class, args);
    }
}
