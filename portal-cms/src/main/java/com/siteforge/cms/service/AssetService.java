package com.siteforge.cms.service;

import com.siteforge.cms.dto.AssetRequest;
import com.siteforge.cms.dto.AssetResponse;
import com.siteforge.cms.storage.StorageResult;
import com.siteforge.cms.storage.StorageService;
import com.siteforge.domain.entity.Asset;
import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AssetService {

    private final AssetRepository assetRepository;
    private final StorageService storageService;
    private final CmsUserService cmsUserService;

    @Transactional(readOnly = true)
    public List<AssetResponse> findAll() {
        return assetRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toResponse).toList();
    }

    public AssetResponse create(AssetRequest request, String username) {
        // 驗證必填欄位，防止 DB not-null 約束錯誤
        if (request.getFilename() == null || request.getFilename().isBlank())
            throw new IllegalArgumentException("filename is required");
        if (request.getFilePath() == null || request.getFilePath().isBlank())
            throw new IllegalArgumentException("filePath is required");
        Asset asset = new Asset();
        asset.setFilename(request.getFilename());
        asset.setFilePath(request.getFilePath());
        asset.setMimeType(request.getMimeType());
        asset.setSize(request.getSize());
        asset.setCreatedBy(username);
        return toResponse(assetRepository.save(asset));
    }

    public AssetResponse upload(MultipartFile file, String username) {
        StorageResult result = storageService.store(file);
        // 補 unit 與 uploadedBy，確保 existsByIdAndUnitCode() 查詢能正確比對
        CmsUser user = cmsUserService.loadUser(username);
        Asset asset = new Asset();
        asset.setFilename(result.originalFilename());
        asset.setFilePath(result.filePath());
        asset.setMimeType(result.mimeType());
        asset.setSize(result.size());
        asset.setUnit(user.getUnit());
        asset.setUploadedBy(username);
        asset.setCreatedBy(username);
        return toResponse(assetRepository.save(asset));
    }

    private AssetResponse toResponse(Asset a) {
        return new AssetResponse(a.getId(), a.getFilename(), a.getFilePath(),
            a.getMimeType(), a.getSize(), a.getCreatedAt(), a.getCreatedBy());
    }
}
