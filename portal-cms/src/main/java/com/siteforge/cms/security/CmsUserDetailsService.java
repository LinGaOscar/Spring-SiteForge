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


@Service
@RequiredArgsConstructor
public class CmsUserDetailsService implements UserDetailsService {

    private final CmsUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        // 從資料庫查詢使用者，找不到則拋出標準 Spring Security 例外
        CmsUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // 將 CmsRole 集合轉換為 Spring Security 的 GrantedAuthority
        var authorities = user.getRoles().stream()
            .map(CmsRole::getName)
            .map(SimpleGrantedAuthority::new)
            .toList();

        return User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(authorities)
            .disabled(!user.getEnabled())
            .build();
    }
}
