package com.siteforge.cms.service;

import com.siteforge.cms.dto.AssetRequest;
import com.siteforge.cms.dto.AssetResponse;
import com.siteforge.domain.entity.Asset;
import com.siteforge.domain.repository.AssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock AssetRepository assetRepository;
    @InjectMocks AssetService assetService;

    @Test
    void create_validRequest_returnsSavedAsset() {
        AssetRequest request = new AssetRequest();
        request.setFilename("logo.png");
        request.setFilePath("/uploads/logo.png");
        request.setMimeType("image/png");
        request.setSize(2048L);

        Asset saved = new Asset();
        saved.setId(1L); saved.setFilename("logo.png");
        saved.setFilePath("/uploads/logo.png"); saved.setMimeType("image/png");
        saved.setSize(2048L); saved.setCreatedBy("manager");
        saved.setCreatedAt(LocalDateTime.now());
        when(assetRepository.save(any())).thenReturn(saved);

        AssetResponse response = assetService.create(request, "manager");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.filename()).isEqualTo("logo.png");
        assertThat(response.createdBy()).isEqualTo("manager");
    }

    @Test
    void create_missingFilename_throwsIllegalArgument() {
        AssetRequest request = new AssetRequest();
        request.setFilePath("/uploads/file.png");

        assertThatThrownBy(() -> assetService.create(request, "manager"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("filename is required");
    }

    @Test
    void create_missingFilePath_throwsIllegalArgument() {
        AssetRequest request = new AssetRequest();
        request.setFilename("file.png");

        assertThatThrownBy(() -> assetService.create(request, "manager"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("filePath is required");
    }

    @Test
    void findAll_returnsAllAssets() {
        Asset a = new Asset(); a.setId(1L); a.setFilename("logo.png");
        a.setFilePath("/uploads/logo.png"); a.setCreatedAt(LocalDateTime.now());
        when(assetRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(a));

        assertThat(assetService.findAll()).hasSize(1);
    }
}
