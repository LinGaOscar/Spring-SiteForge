package com.siteforge.cms.controller;

import com.siteforge.cms.service.CmsUserService;
import com.siteforge.cms.storage.StorageService;
import com.siteforge.domain.entity.Asset;
import com.siteforge.domain.entity.CmsUser;
import com.siteforge.domain.enums.CmsUserRole;
import com.siteforge.domain.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cms/assets")
@RequiredArgsConstructor
public class CmsAssetsViewController {

    private final CmsUserService cmsUserService;
    private final AssetRepository assetRepository;
    private final StorageService storageService;

    @Value("${cms.asset-base-url:}")
    private String assetBaseUrl;

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails ud, Model model) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        // 全部素材對所有單位可見
        model.addAttribute("actor", actor);
        model.addAttribute("assets", assetRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("assetBaseUrl", assetBaseUrl);
        model.addAttribute("unitCode", cmsUserService.unitCode(actor));
        model.addAttribute("isOP", actor.getRoles().contains(CmsUserRole.OP));
        model.addAttribute("isMA", actor.getRoles().contains(CmsUserRole.MA));
        return "cms/assets/list";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam MultipartFile file,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        boolean canUpload = actor.getRoles().contains(CmsUserRole.OP)
                         || actor.getRoles().contains(CmsUserRole.MA);
        if (!canUpload) {
            ra.addFlashAttribute("error", "無上傳權限");
            return "redirect:/cms/assets";
        }
        try {
            var result = storageService.store(file);
            Asset asset = new Asset();
            asset.setFilename(result.originalFilename());
            asset.setFilePath(result.filePath());
            asset.setMimeType(file.getContentType());
            asset.setSize(file.getSize());
            asset.setUnit(actor.getUnit());
            asset.setCreatedBy(actor.getUsername());
            asset.setUploadedBy(actor.getUsername());
            assetRepository.save(asset);
            ra.addFlashAttribute("success", "上傳成功：" + result.originalFilename());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "上傳失敗：" + e.getMessage());
        }
        return "redirect:/cms/assets";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra) {
        CmsUser actor = cmsUserService.loadUser(ud.getUsername());
        String unitCode = cmsUserService.unitCode(actor);

        boolean canDelete = (actor.getRoles().contains(CmsUserRole.OP)
                          || actor.getRoles().contains(CmsUserRole.MA))
                         && assetRepository.existsByIdAndUnitCode(id, unitCode);
        if (!canDelete) {
            ra.addFlashAttribute("error", "無刪除權限（只能刪除本單位上傳的素材）");
            return "redirect:/cms/assets";
        }
        assetRepository.deleteById(id);
        ra.addFlashAttribute("success", "素材已刪除");
        return "redirect:/cms/assets";
    }
}
