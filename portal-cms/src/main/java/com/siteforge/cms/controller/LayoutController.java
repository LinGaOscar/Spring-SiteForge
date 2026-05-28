package com.siteforge.cms.controller;

import com.siteforge.cms.common.ApiResponse;
import com.siteforge.cms.dto.LayoutSetRequest;
import com.siteforge.cms.dto.LayoutSetResponse;
import com.siteforge.cms.service.LayoutSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/layouts")
@RequiredArgsConstructor
public class LayoutController {

    private final LayoutSetService layoutSetService;

    @GetMapping
    public ApiResponse<List<LayoutSetResponse>> list() {
        return ApiResponse.ok(layoutSetService.findAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LayoutSetResponse> create(@RequestBody LayoutSetRequest request,
                                                  Authentication authentication) {
        return ApiResponse.ok(layoutSetService.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ApiResponse<LayoutSetResponse> update(@PathVariable Long id,
                                                  @RequestBody LayoutSetRequest request,
                                                  Authentication authentication) {
        return ApiResponse.ok(layoutSetService.update(id, request, authentication.getName()));
    }
}
