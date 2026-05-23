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
