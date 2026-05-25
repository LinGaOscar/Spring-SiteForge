package com.siteforge.cms.controller;

import com.siteforge.cms.common.ApiResponse;
import com.siteforge.cms.dto.PageVersionResponse;
import com.siteforge.cms.dto.RollbackRequest;
import com.siteforge.cms.service.PublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cms/pages/{pageId}")
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;

    @PostMapping("/publish")
    @PreAuthorize("hasRole('MA')")
    public ApiResponse<PageVersionResponse> publish(@PathVariable Long pageId,
                                                     Authentication authentication) {
        return ApiResponse.ok(publishService.publish(pageId, authentication.getName()));
    }

    @PostMapping("/rollback")
    @PreAuthorize("hasRole('MA')")
    public ApiResponse<PageVersionResponse> rollback(@PathVariable Long pageId,
                                                      @RequestBody RollbackRequest request,
                                                      Authentication authentication) {
        return ApiResponse.ok(publishService.rollback(pageId, request.getVersionId(), authentication.getName()));
    }

    @GetMapping("/versions")
    public ApiResponse<List<PageVersionResponse>> versions(@PathVariable Long pageId) {
        return ApiResponse.ok(publishService.listVersions(pageId));
    }
}
