package com.siteforge.cms.dto;

import com.siteforge.domain.enums.TemplateKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class LayoutSetRequest {
    private String name;
    private TemplateKey headerKey;
    private TemplateKey bodyKey;
    private TemplateKey footerKey;
    private String description;
    private Boolean enabled = true;
}
