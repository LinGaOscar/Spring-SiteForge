package com.siteforge.cms.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class AssetRequest {
    private String filename;
    private String filePath;
    private String mimeType;
    private Long size;
}
