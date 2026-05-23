package com.siteforge.cms.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class PageRequest {
    private Long siteId;
    private String path;
    private String title;
    private String seoTitle;
    private String seoDescription;
    private Long layoutSetId;
}
