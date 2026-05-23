package com.siteforge.cms.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class RollbackRequest {
    private Long versionId;
}
