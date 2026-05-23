# Phase 1: Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 Maven multi-module 骨架（portal-domain / portal-web / portal-cms）、PostgreSQL schema with Flyway、portal-cms Spring Security 表單登入、以及統一 ApiResponse 格式。

**Architecture:** 三個 Maven 模組共用一個 parent POM。portal-domain 是純 JPA library（無 Spring Boot main class）。portal-web（port 8100）與 portal-cms（port 8200）各自是 Spring Boot 應用，都透過 `@EntityScan` + `@EnableJpaRepositories` 引用 portal-domain 的 entity 與 repository。portal-cms 負責執行 Flyway migration；portal-web 關閉 Flyway。

**Tech Stack:** Java 21、Spring Boot 3.3.4、Spring Data JPA、PostgreSQL 16、Flyway 10.x、Spring Security 6、Thymeleaf 3、Lombok、H2（test）、Maven 3.9+

---

## 前置：本機 PostgreSQL

```bash
docker run -d \
  --name siteforge-postgres \
  -e POSTGRES_DB=siteforge_db \
  -e POSTGRES_USER=siteforge \
  -e POSTGRES_PASSWORD=siteforge \
  -p 5432:5432 \
  postgres:16
```

---

## 檔案結構總覽

```
Spring-SiteForge/
├── pom.xml                                          # Parent POM
├── portal-domain/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/siteforge/domain/
│       │   ├── entity/CmsUser.java
│       │   ├── entity/CmsRole.java
│       │   ├── repository/CmsUserRepository.java
│       │   ├── repository/CmsRoleRepository.java
│       │   ├── enums/PageStatus.java
│       │   └── enums/TemplateKey.java
│       └── test/java/com/siteforge/domain/
│           ├── DomainTestApplication.java
│           └── repository/CmsUserRepositoryTest.java
├── portal-web/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/siteforge/web/PortalWebApplication.java
│       └── resources/application.yml
│       └── resources/application-dev.yml
└── portal-cms/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/siteforge/cms/
        │   │   ├── PortalCmsApplication.java
        │   │   ├── security/SecurityConfig.java
        │   │   ├── security/CmsUserDetailsService.java
        │   │   ├── controller/AuthController.java
        │   │   ├── controller/DashboardController.java
        │   │   ├── common/ApiResponse.java
        │   │   ├── common/ErrorDetail.java
        │   │   ├── common/GlobalExceptionHandler.java
        │   │   └── init/DataInitializer.java
        │   └── resources/
        │       ├── application.yml
        │       ├── application-dev.yml
        │       ├── db/migration/V1__create_cms_user_tables.sql
        │       └── templates/
        │           ├── layout/base.html
        │           └── cms/auth/login.html
        │           └── cms/dashboard.html
        └── test/java/com/siteforge/cms/
            ├── security/CmsUserDetailsServiceTest.java
            └── common/ApiResponseTest.java
```

---

## Task 1: Maven Parent POM + Module 目錄

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: 建立 parent pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.4</version>
    <relativePath/>
  </parent>

  <groupId>com.siteforge</groupId>
  <artifactId>spring-siteforge</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <modules>
    <module>portal-domain</module>
    <module>portal-web</module>
    <module>portal-cms</module>
  </modules>

  <properties>
    <java.version>21</java.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.siteforge</groupId>
        <artifactId>portal-domain</artifactId>
        <version>${project.version}</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <configuration>
            <annotationProcessorPaths>
              <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
              </path>
            </annotationProcessorPaths>
          </configuration>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

- [ ] **Step 2: 建立三個模組目錄**

```bash
mkdir -p portal-domain/src/main/java/com/siteforge/domain/{entity,repository,enums}
mkdir -p portal-domain/src/test/java/com/siteforge/domain/repository
mkdir -p portal-web/src/main/java/com/siteforge/web
mkdir -p portal-web/src/main/resources
mkdir -p portal-cms/src/main/java/com/siteforge/cms/{security,controller,common,init}
mkdir -p portal-cms/src/main/resources/{db/migration,templates/{layout,cms/auth}}
mkdir -p portal-cms/src/test/java/com/siteforge/cms/{security,common}
```

---

## Task 2: portal-domain POM

**Files:**
- Create: `portal-domain/pom.xml`

- [ ] **Step 1: 建立 portal-domain/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.siteforge</groupId>
    <artifactId>spring-siteforge</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>portal-domain</artifactId>
  <packaging>jar</packaging>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>

    <!-- Test -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

---

## Task 3: portal-domain — Entities + Repositories + Enums

**Files:**
- Create: `portal-domain/src/main/java/com/siteforge/domain/entity/CmsRole.java`
- Create: `portal-domain/src/main/java/com/siteforge/domain/entity/CmsUser.java`
- Create: `portal-domain/src/main/java/com/siteforge/domain/repository/CmsRoleRepository.java`
- Create: `portal-domain/src/main/java/com/siteforge/domain/repository/CmsUserRepository.java`
- Create: `portal-domain/src/main/java/com/siteforge/domain/enums/PageStatus.java`
- Create: `portal-domain/src/main/java/com/siteforge/domain/enums/TemplateKey.java`
- Create: `portal-domain/src/test/java/com/siteforge/domain/DomainTestApplication.java`
- Create: `portal-domain/src/test/java/com/siteforge/domain/repository/CmsUserRepositoryTest.java`

- [ ] **Step 1: 撰寫失敗測試**

```java
// portal-domain/src/test/java/com/siteforge/domain/repository/CmsUserRepositoryTest.java
package com.siteforge.domain.repository;

import com.siteforge.domain.entity.CmsUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CmsUserRepositoryTest {

    @Autowired
    CmsUserRepository repo;

    @Test
    void saveAndFindByUsername() {
        CmsUser user = new CmsUser();
        user.setUsername("tester");
        user.setPassword("hashed");
        user.setEnabled(true);
        repo.save(user);

        Optional<CmsUser> found = repo.findByUsername("tester");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("tester");
    }

    @Test
    void findByUsername_notFound_returnsEmpty() {
        assertThat(repo.findByUsername("nobody")).isEmpty();
    }
}
```

- [ ] **Step 2: 建立 DomainTestApplication（讓 @DataJpaTest 能找到 entity）**

```java
// portal-domain/src/test/java/com/siteforge/domain/DomainTestApplication.java
package com.siteforge.domain;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DomainTestApplication { }
```

- [ ] **Step 3: 執行測試，確認失敗**

```bash
mvn test -pl portal-domain
```

預期錯誤：`CmsUser` / `CmsUserRepository` class not found

- [ ] **Step 4: 建立 CmsRole entity**

```java
// portal-domain/src/main/java/com/siteforge/domain/entity/CmsRole.java
package com.siteforge.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cms_role")
@Getter @Setter @NoArgsConstructor
public class CmsRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
```

- [ ] **Step 5: 建立 CmsUser entity**

```java
// portal-domain/src/main/java/com/siteforge/domain/entity/CmsUser.java
package com.siteforge.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cms_user")
@Getter @Setter @NoArgsConstructor
public class CmsUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "cms_user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<CmsRole> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 6: 建立 Repositories**

```java
// portal-domain/src/main/java/com/siteforge/domain/repository/CmsUserRepository.java
package com.siteforge.domain.repository;

import com.siteforge.domain.entity.CmsUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CmsUserRepository extends JpaRepository<CmsUser, Long> {
    Optional<CmsUser> findByUsername(String username);
}
```

```java
// portal-domain/src/main/java/com/siteforge/domain/repository/CmsRoleRepository.java
package com.siteforge.domain.repository;

import com.siteforge.domain.entity.CmsRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CmsRoleRepository extends JpaRepository<CmsRole, Long> {
    Optional<CmsRole> findByName(String name);
}
```

- [ ] **Step 7: 建立 Enums**

```java
// portal-domain/src/main/java/com/siteforge/domain/enums/PageStatus.java
package com.siteforge.domain.enums;

public enum PageStatus {
    DRAFT, PUBLISHED
}
```

```java
// portal-domain/src/main/java/com/siteforge/domain/enums/TemplateKey.java
package com.siteforge.domain.enums;

public enum TemplateKey {
    HEADER_DEFAULT,
    FOOTER_DEFAULT,
    BODY_STANDARD,
    BODY_LANDING
}
```

- [ ] **Step 8: 執行測試，確認通過**

```bash
mvn test -pl portal-domain
```

預期：BUILD SUCCESS，2 tests passed

- [ ] **Step 9: Commit**

```bash
git add portal-domain
git commit -m "feat(domain): add CmsUser/CmsRole entities, repositories, and enums"
```

---

## Task 4: portal-cms — POM + Application + Flyway

**Files:**
- Create: `portal-cms/pom.xml`
- Create: `portal-cms/src/main/java/com/siteforge/cms/PortalCmsApplication.java`
- Create: `portal-cms/src/main/resources/application.yml`
- Create: `portal-cms/src/main/resources/application-dev.yml`
- Create: `portal-cms/src/main/resources/db/migration/V1__create_cms_user_tables.sql`
- Create: `portal-cms/src/main/java/com/siteforge/cms/init/DataInitializer.java`

- [ ] **Step 1: 建立 portal-cms/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.siteforge</groupId>
    <artifactId>spring-siteforge</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>portal-cms</artifactId>

  <dependencies>
    <dependency>
      <groupId>com.siteforge</groupId>
      <artifactId>portal-domain</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
      <groupId>org.thymeleaf.extras</groupId>
      <artifactId>thymeleaf-extras-springsecurity6</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-devtools</artifactId>
      <scope>runtime</scope>
      <optional>true</optional>
    </dependency>

    <!-- Test -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: 建立 PortalCmsApplication**

```java
// portal-cms/src/main/java/com/siteforge/cms/PortalCmsApplication.java
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
```

- [ ] **Step 3: 建立 application.yml**

```yaml
# portal-cms/src/main/resources/application.yml
spring:
  application:
    name: portal-cms
  jpa:
    open-in-view: false

server:
  port: 8200
```

- [ ] **Step 4: 建立 application-dev.yml**

```yaml
# portal-cms/src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/siteforge_db
    username: siteforge
    password: siteforge
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
```

- [ ] **Step 5: 建立 Flyway migration V1**

```sql
-- portal-cms/src/main/resources/db/migration/V1__create_cms_user_tables.sql

CREATE TABLE cms_role (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE cms_user (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE cms_user_role (
    user_id BIGINT NOT NULL REFERENCES cms_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES cms_role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);
```

- [ ] **Step 6: 建立 DataInitializer（dev 環境初始使用者）**

```java
// portal-cms/src/main/java/com/siteforge/cms/init/DataInitializer.java
package com.siteforge.cms.init;

import com.siteforge.domain.entity.CmsRole;
import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.repository.CmsRoleRepository;
import com.siteforge.domain.repository.CmsUserRepository;
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

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) return;

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
}
```

- [ ] **Step 7: 確認模組能編譯**

```bash
mvn compile -pl portal-cms -am
```

預期：BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add portal-cms/pom.xml portal-cms/src/main/java portal-cms/src/main/resources
git commit -m "feat(cms): scaffold portal-cms with Flyway migration and dev seed"
```

---

## Task 5: portal-web — POM + Application

**Files:**
- Create: `portal-web/pom.xml`
- Create: `portal-web/src/main/java/com/siteforge/web/PortalWebApplication.java`
- Create: `portal-web/src/main/resources/application.yml`
- Create: `portal-web/src/main/resources/application-dev.yml`

- [ ] **Step 1: 建立 portal-web/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.siteforge</groupId>
    <artifactId>spring-siteforge</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>portal-web</artifactId>

  <dependencies>
    <dependency>
      <groupId>com.siteforge</groupId>
      <artifactId>portal-domain</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-devtools</artifactId>
      <scope>runtime</scope>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: 建立 PortalWebApplication**

```java
// portal-web/src/main/java/com/siteforge/web/PortalWebApplication.java
package com.siteforge.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.siteforge.domain.entity")
@EnableJpaRepositories("com.siteforge.domain.repository")
public class PortalWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(PortalWebApplication.class, args);
    }
}
```

- [ ] **Step 3: 建立 application.yml**

```yaml
# portal-web/src/main/resources/application.yml
spring:
  application:
    name: portal-web
  jpa:
    open-in-view: false
  flyway:
    enabled: false

server:
  port: 8100
```

- [ ] **Step 4: 建立 application-dev.yml**

```yaml
# portal-web/src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/siteforge_db
    username: siteforge
    password: siteforge
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
```

- [ ] **Step 5: 確認模組編譯**

```bash
mvn compile -pl portal-web -am
```

預期：BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add portal-web
git commit -m "feat(web): scaffold portal-web module"
```

---

## Task 6: portal-cms — Spring Security

**Files:**
- Create: `portal-cms/src/main/java/com/siteforge/cms/security/CmsUserDetailsService.java`
- Create: `portal-cms/src/main/java/com/siteforge/cms/security/SecurityConfig.java`
- Create: `portal-cms/src/test/java/com/siteforge/cms/security/CmsUserDetailsServiceTest.java`

- [ ] **Step 1: 撰寫 CmsUserDetailsService 失敗測試**

```java
// portal-cms/src/test/java/com/siteforge/cms/security/CmsUserDetailsServiceTest.java
package com.siteforge.cms.security;

import com.siteforge.domain.entity.CmsRole;
import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.repository.CmsUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CmsUserDetailsServiceTest {

    @Mock
    CmsUserRepository userRepository;

    @InjectMocks
    CmsUserDetailsService service;

    @Test
    void loadUserByUsername_found_returnsUserDetails() {
        CmsRole role = new CmsRole();
        role.setName("ROLE_MANAGER");

        CmsUser user = new CmsUser();
        user.setUsername("manager");
        user.setPassword("$2a$10$hashed");
        user.setEnabled(true);
        user.setRoles(Set.of(role));

        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("manager");

        assertThat(details.getUsername()).isEqualTo("manager");
        assertThat(details.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_MANAGER");
    }

    @Test
    void loadUserByUsername_notFound_throwsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("ghost");
    }
}
```

- [ ] **Step 2: 執行測試，確認失敗**

```bash
mvn test -pl portal-cms -Dtest=CmsUserDetailsServiceTest
```

預期錯誤：`CmsUserDetailsService` class not found

- [ ] **Step 3: 實作 CmsUserDetailsService**

```java
// portal-cms/src/main/java/com/siteforge/cms/security/CmsUserDetailsService.java
package com.siteforge.cms.security;

import com.siteforge.domain.entity.CmsRole;
import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.repository.CmsUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CmsUserDetailsService implements UserDetailsService {

    private final CmsUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        CmsUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        var authorities = user.getRoles().stream()
            .map(CmsRole::getName)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        return User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(authorities)
            .disabled(!user.getEnabled())
            .build();
    }
}
```

- [ ] **Step 4: 執行測試，確認通過**

```bash
mvn test -pl portal-cms -Dtest=CmsUserDetailsServiceTest
```

預期：BUILD SUCCESS，2 tests passed

- [ ] **Step 5: 實作 SecurityConfig**

```java
// portal-cms/src/main/java/com/siteforge/cms/security/SecurityConfig.java
package com.siteforge.cms.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CmsUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/cms/auth/login", "/css/**", "/js/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/cms/auth/login")
                .loginProcessingUrl("/cms/auth/login")
                .defaultSuccessUrl("/cms/dashboard", true)
                .failureUrl("/cms/auth/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/cms/auth/logout")
                .logoutSuccessUrl("/cms/auth/login?logout")
                .permitAll()
            )
            .authenticationProvider(authenticationProvider());
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add portal-cms/src/main/java/com/siteforge/cms/security \
        portal-cms/src/test/java/com/siteforge/cms/security
git commit -m "feat(cms): add Spring Security with form login"
```

---

## Task 7: portal-cms — Login UI + Dashboard

**Files:**
- Create: `portal-cms/src/main/java/com/siteforge/cms/controller/AuthController.java`
- Create: `portal-cms/src/main/java/com/siteforge/cms/controller/DashboardController.java`
- Create: `portal-cms/src/main/resources/templates/layout/base.html`
- Create: `portal-cms/src/main/resources/templates/cms/auth/login.html`
- Create: `portal-cms/src/main/resources/templates/cms/dashboard.html`

- [ ] **Step 1: 建立 AuthController**

```java
// portal-cms/src/main/java/com/siteforge/cms/controller/AuthController.java
package com.siteforge.cms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cms/auth")
public class AuthController {

    @GetMapping("/login")
    public String loginPage() {
        return "cms/auth/login";
    }
}
```

- [ ] **Step 2: 建立 DashboardController**

```java
// portal-cms/src/main/java/com/siteforge/cms/controller/DashboardController.java
package com.siteforge.cms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/cms/dashboard")
    public String dashboard() {
        return "cms/dashboard";
    }
}
```

- [ ] **Step 3: 建立 base.html layout**

```html
<!-- portal-cms/src/main/resources/templates/layout/base.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title th:text="${pageTitle} ?: 'SiteForge CMS'">SiteForge CMS</title>
</head>
<body>
  <nav th:fragment="nav">
    <span sec:authentication="name">User</span>
    <a th:href="@{/cms/auth/logout}">登出</a>
  </nav>
  <div th:fragment="content">
    <!-- page content here -->
  </div>
</body>
</html>
```

- [ ] **Step 4: 建立 login.html**

```html
<!-- portal-cms/src/main/resources/templates/cms/auth/login.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <title>登入 — SiteForge CMS</title>
</head>
<body>
  <h1>SiteForge CMS</h1>

  <div th:if="${param.error}">
    <p style="color:red">帳號或密碼錯誤</p>
  </div>
  <div th:if="${param.logout}">
    <p>已成功登出</p>
  </div>

  <form th:action="@{/cms/auth/login}" method="post">
    <label>帳號
      <input type="text" name="username" autocomplete="username" required>
    </label>
    <label>密碼
      <input type="password" name="password" autocomplete="current-password" required>
    </label>
    <button type="submit">登入</button>
  </form>
</body>
</html>
```

- [ ] **Step 5: 建立 dashboard.html**

```html
<!-- portal-cms/src/main/resources/templates/cms/dashboard.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
  <meta charset="UTF-8">
  <title>Dashboard — SiteForge CMS</title>
</head>
<body>
  <h1>SiteForge CMS Dashboard</h1>
  <p>歡迎，<span sec:authentication="name"></span></p>
  <a th:href="@{/cms/auth/logout}">登出</a>
</body>
</html>
```

- [ ] **Step 6: Commit**

```bash
git add portal-cms/src/main/java/com/siteforge/cms/controller \
        portal-cms/src/main/resources/templates
git commit -m "feat(cms): add login page and dashboard"
```

---

## Task 8: portal-cms — ApiResponse + GlobalExceptionHandler

**Files:**
- Create: `portal-cms/src/main/java/com/siteforge/cms/common/ErrorDetail.java`
- Create: `portal-cms/src/main/java/com/siteforge/cms/common/ApiResponse.java`
- Create: `portal-cms/src/main/java/com/siteforge/cms/common/GlobalExceptionHandler.java`
- Create: `portal-cms/src/test/java/com/siteforge/cms/common/ApiResponseTest.java`

- [ ] **Step 1: 撰寫 ApiResponse 失敗測試**

```java
// portal-cms/src/test/java/com/siteforge/cms/common/ApiResponseTest.java
package com.siteforge.cms.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ok_setsSuccessAndData() {
        ApiResponse<String> res = ApiResponse.ok("hello");

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isEqualTo("hello");
        assertThat(res.getError()).isNull();
    }

    @Test
    void ok_withNullData_isStillSuccess() {
        ApiResponse<Void> res = ApiResponse.ok(null);

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isNull();
    }

    @Test
    void error_setsFailureAndErrorDetail() {
        ApiResponse<Void> res = ApiResponse.error("NOT_FOUND", "Resource not found");

        assertThat(res.isSuccess()).isFalse();
        assertThat(res.getData()).isNull();
        assertThat(res.getError().getCode()).isEqualTo("NOT_FOUND");
        assertThat(res.getError().getMessage()).isEqualTo("Resource not found");
    }
}
```

- [ ] **Step 2: 執行測試，確認失敗**

```bash
mvn test -pl portal-cms -Dtest=ApiResponseTest
```

預期錯誤：`ApiResponse` class not found

- [ ] **Step 3: 實作 ErrorDetail**

```java
// portal-cms/src/main/java/com/siteforge/cms/common/ErrorDetail.java
package com.siteforge.cms.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorDetail {
    private final String code;
    private final String message;
}
```

- [ ] **Step 4: 實作 ApiResponse**

```java
// portal-cms/src/main/java/com/siteforge/cms/common/ApiResponse.java
package com.siteforge.cms.common;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetail error;

    private ApiResponse(boolean success, T data, ErrorDetail error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message));
    }
}
```

- [ ] **Step 5: 執行測試，確認通過**

```bash
mvn test -pl portal-cms -Dtest=ApiResponseTest
```

預期：BUILD SUCCESS，3 tests passed

- [ ] **Step 6: 實作 GlobalExceptionHandler**

```java
// portal-cms/src/main/java/com/siteforge/cms/common/GlobalExceptionHandler.java
package com.siteforge.cms.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "伺服器發生錯誤，請稍後再試"));
    }
}
```

- [ ] **Step 7: 執行全部測試**

```bash
mvn test -pl portal-cms
```

預期：BUILD SUCCESS，所有測試通過

- [ ] **Step 8: Commit**

```bash
git add portal-cms/src/main/java/com/siteforge/cms/common \
        portal-cms/src/test/java/com/siteforge/cms/common
git commit -m "feat(cms): add ApiResponse and GlobalExceptionHandler"
```

---

## Task 9: Smoke Test — 啟動兩個應用

- [ ] **Step 1: 確認 PostgreSQL 運行中**

```bash
docker ps | grep siteforge-postgres
```

預期：容器狀態為 Up

- [ ] **Step 2: 啟動 portal-cms**

```bash
mvn spring-boot:run -pl portal-cms -Dspring-boot.run.profiles=dev
```

預期 log：
```
Flyway migration: V1__create_cms_user_tables.sql — completed
=== Dev seed: manager / siteforge2026 ===
Started PortalCmsApplication on port 8200
```

- [ ] **Step 3: 瀏覽器驗證登入**

開啟 `http://localhost:8200/cms/dashboard`
→ 應被重導至 `http://localhost:8200/cms/auth/login`

輸入 `manager` / `siteforge2026` → 登入成功，出現 Dashboard 頁面

- [ ] **Step 4: 啟動 portal-web（另一個 terminal）**

```bash
mvn spring-boot:run -pl portal-web -Dspring-boot.run.profiles=dev
```

預期 log：`Started PortalWebApplication on port 8100`（Flyway 不執行）

- [ ] **Step 5: 全專案測試**

```bash
mvn test
```

預期：BUILD SUCCESS，所有模組測試通過

- [ ] **Step 6: 最終 commit + push**

```bash
git add .
git commit -m "chore: Phase 1 complete — multi-module scaffold, login, ApiResponse"
git push
```

---

## Phase 1 驗收清單

- [ ] `mvn test` 全部通過
- [ ] portal-cms 可在 8200 啟動，Flyway migration 成功執行
- [ ] portal-web 可在 8100 啟動，Flyway 不執行
- [ ] 瀏覽 `/cms/dashboard` → 重導至 login page
- [ ] 以 `manager / siteforge2026` 登入 → 進入 dashboard
- [ ] 登出後回到 login page
