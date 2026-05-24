package com.siteforge.cms.service;

import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.enums.CmsUserRole;
import com.siteforge.domain.repository.CmsUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CmsUserService {

    private final CmsUserRepository userRepository;

    @Transactional(readOnly = true)
    public CmsUser loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public boolean hasRole(CmsUser user, CmsUserRole role) {
        return user.getRoles().contains(role);
    }

    public String unitCode(CmsUser user) {
        return user.getUnit() != null ? user.getUnit().getCode() : null;
    }
}
