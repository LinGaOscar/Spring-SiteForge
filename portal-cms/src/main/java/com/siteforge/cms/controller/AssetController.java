package com.siteforge.cms.controller;

import com.siteforge.cms.common.ApiResponse;
import com.siteforge.cms.dto.AssetRequest;
import com.siteforge.cms.dto.AssetResponse;
import com.siteforge.cms.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public ApiResponse<List<AssetResponse>> list() {
        return ApiResponse.ok(assetService.findAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AssetResponse> create(@RequestBody AssetRequest request,
                                              Authentication authentication) {
        return ApiResponse.ok(assetService.create(request, authentication.getName()));
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AssetResponse> upload(@RequestParam("file") MultipartFile file,
                                              Authentication authentication) {
        return ApiResponse.ok(assetService.upload(file, authentication.getName()));
    }
}
