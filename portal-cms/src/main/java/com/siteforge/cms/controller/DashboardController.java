package com.siteforge.cms.controller;

import com.siteforge.cms.service.CmsUserService;
import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.enums.PageStatus;
import com.siteforge.domain.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final CmsUserService cmsUserService;
    private final PageRepository pageRepository;

    @GetMapping("/cms/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        String unitCode = cmsUserService.unitCode(actor);

        var pages = pageRepository.findByUnitCode(unitCode);
        Map<PageStatus, Long> stats = pages.stream()
                .collect(Collectors.groupingBy(com.siteforge.domain.entity.Page::getStatus, Collectors.counting()));

        // MA 待處理：非本人送出的審核項目
        var pending = pageRepository.findByUnitCodeAndStatusInOrderBySubmittedAtDesc(
                unitCode,
                List.of(PageStatus.PENDING_REVIEW, PageStatus.PENDING_PUBLISH, PageStatus.PENDING_UNPUBLISH))
                .stream()
                .filter(p -> !ud.getUsername().equals(p.getSubmittedBy()))
                .toList();

        model.addAttribute("actor", actor);
        model.addAttribute("stats", stats);
        model.addAttribute("pending", pending);
        model.addAttribute("totalPages", pages.size());
        model.addAttribute("isMA", actor.getRoles().contains(com.siteforge.domain.enums.CmsUserRole.MA));
        return "cms/dashboard";
    }
}
