package com.siteforge.cms.controller;

import com.siteforge.cms.service.CmsUserService;
import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.repository.ComponentDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cms/components")
@RequiredArgsConstructor
public class CmsComponentsViewController {

    private final ComponentDefinitionRepository componentDefinitionRepository;
    private final CmsUserService cmsUserService;

    @Value("${cms.preview-base-url:}")
    private String previewBaseUrl;

    // CMS 元件管理列表
    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails ud, Model model) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        model.addAttribute("actor", actor);
        model.addAttribute("components", componentDefinitionRepository.findAll());
        model.addAttribute("previewBaseUrl", previewBaseUrl);
        return "cms/components/list";
    }
}
