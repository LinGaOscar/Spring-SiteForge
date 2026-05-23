package com.siteforge.cms.service;

import com.siteforge.cms.dto.LayoutSetRequest;
import com.siteforge.cms.dto.LayoutSetResponse;
import com.siteforge.domain.entity.LayoutSet;
import com.siteforge.domain.repository.LayoutSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LayoutSetService {

    private final LayoutSetRepository layoutSetRepository;

    @Transactional(readOnly = true)
    public List<LayoutSetResponse> findAll() {
        return layoutSetRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LayoutSetResponse findById(Long id) {
        return layoutSetRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new IllegalArgumentException("LayoutSet not found: " + id));
    }

    public LayoutSetResponse create(LayoutSetRequest request, String username) {
        validateTemplateKeys(request);
        LayoutSet layoutSet = new LayoutSet();
        applyRequest(layoutSet, request);
        layoutSet.setCreatedBy(username);
        layoutSet.setUpdatedBy(username);
        return toResponse(layoutSetRepository.save(layoutSet));
    }

    public LayoutSetResponse update(Long id, LayoutSetRequest request, String username) {
        validateTemplateKeys(request);
        LayoutSet layoutSet = layoutSetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("LayoutSet not found: " + id));
        applyRequest(layoutSet, request);
        layoutSet.setUpdatedBy(username);
        return toResponse(layoutSetRepository.save(layoutSet));
    }

    private void applyRequest(LayoutSet layoutSet, LayoutSetRequest request) {
        layoutSet.setName(request.getName());
        layoutSet.setHeaderKey(request.getHeaderKey());
        layoutSet.setBodyKey(request.getBodyKey());
        layoutSet.setFooterKey(request.getFooterKey());
        layoutSet.setDescription(request.getDescription());
        if (request.getEnabled() != null) layoutSet.setEnabled(request.getEnabled());
    }

    // 確保各插槽只能使用對應前綴的 TemplateKey，避免前端誤傳錯誤 key
    private void validateTemplateKeys(LayoutSetRequest request) {
        if (request.getHeaderKey() == null || !request.getHeaderKey().name().startsWith("HEADER_"))
            throw new IllegalArgumentException("headerKey must be a HEADER_* TemplateKey");
        if (request.getBodyKey() == null || !request.getBodyKey().name().startsWith("BODY_"))
            throw new IllegalArgumentException("bodyKey must be a BODY_* TemplateKey");
        if (request.getFooterKey() == null || !request.getFooterKey().name().startsWith("FOOTER_"))
            throw new IllegalArgumentException("footerKey must be a FOOTER_* TemplateKey");
    }

    private LayoutSetResponse toResponse(LayoutSet l) {
        return new LayoutSetResponse(l.getId(), l.getName(), l.getHeaderKey(), l.getBodyKey(),
            l.getFooterKey(), l.getDescription(), l.getEnabled(), l.getCreatedAt(), l.getUpdatedAt());
    }
}
