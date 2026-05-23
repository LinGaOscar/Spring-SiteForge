package com.siteforge.cms.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class PageContentRequest {
    private String blockKey;
    private int sortOrder = 0;
    private String contentJson;
    private String locale = "zh-TW";
}
