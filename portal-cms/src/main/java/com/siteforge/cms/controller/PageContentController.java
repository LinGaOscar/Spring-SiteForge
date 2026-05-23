package com.siteforge.cms.controller;

import com.siteforge.cms.common.ApiResponse;
import com.siteforge.cms.dto.PageContentRequest;
import com.siteforge.cms.dto.PageContentResponse;
import com.siteforge.cms.service.PageContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cms/pages/{pageId}/contents")
@RequiredArgsConstructor
public class PageContentController {

    private final PageContentService pageContentService;

    @GetMapping
    public ApiResponse<List<PageContentResponse>> list(@PathVariable Long pageId) {
        return ApiResponse.ok(pageContentService.findByPageId(pageId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PageContentResponse> create(@PathVariable Long pageId,
                                                    @RequestBody PageContentRequest request) {
        return ApiResponse.ok(pageContentService.create(pageId, request));
    }

    @PutMapping("/{contentId}")
    public ApiResponse<PageContentResponse> update(@PathVariable Long pageId,
                                                    @PathVariable Long contentId,
                                                    @RequestBody PageContentRequest request) {
        return ApiResponse.ok(pageContentService.update(pageId, contentId, request));
    }

    @DeleteMapping("/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long pageId, @PathVariable Long contentId) {
        pageContentService.delete(pageId, contentId);
    }
}
