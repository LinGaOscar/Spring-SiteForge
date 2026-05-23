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
