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
            .containsExactlyInAnyOrder("ROLE_MANAGER");
    }

    @Test
    void loadUserByUsername_notFound_throwsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("ghost");
    }
}
