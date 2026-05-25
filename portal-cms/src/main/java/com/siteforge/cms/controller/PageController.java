package com.siteforge.cms.controller;

import com.siteforge.cms.common.ApiResponse;
import com.siteforge.cms.dto.PageRequest;
import com.siteforge.cms.dto.PageResponse;
import com.siteforge.cms.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cms/pages")
@RequiredArgsConstructor
public class PageController {

    private final PageService pageService;

    @GetMapping
    public ApiResponse<List<PageResponse>> list() {
        return ApiResponse.ok(pageService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PageResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(pageService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PageResponse> create(@RequestBody PageRequest request,
                                             Authentication authentication) {
        return ApiResponse.ok(pageService.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ApiResponse<PageResponse> update(@PathVariable Long id,
                                             @RequestBody PageRequest request,
                                             Authentication authentication) {
        return ApiResponse.ok(pageService.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        pageService.delete(id);
    }
}
