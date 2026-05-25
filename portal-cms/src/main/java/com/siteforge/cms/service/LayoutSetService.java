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
        layoutSet.setFooterKey(request.getFooterKey());
        layoutSet.setDescription(request.getDescription());
        if (request.getEnabled() != null) layoutSet.setEnabled(request.getEnabled());
    }

    private void validateTemplateKeys(LayoutSetRequest request) {
        if (request.getHeaderKey() == null || !request.getHeaderKey().name().contains("HEADER"))
            throw new IllegalArgumentException("headerKey must be a HEADER TemplateKey");
        if (request.getFooterKey() == null || !request.getFooterKey().name().contains("FOOTER"))
            throw new IllegalArgumentException("footerKey must be a FOOTER TemplateKey");
    }

    private LayoutSetResponse toResponse(LayoutSet l) {
        return new LayoutSetResponse(l.getId(), l.getName(), l.getHeaderKey(),
            l.getFooterKey(), l.getDescription(), l.getEnabled(), l.getCreatedAt(), l.getUpdatedAt());
    }
}
