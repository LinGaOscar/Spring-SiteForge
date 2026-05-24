package com.siteforge.domain.entity;

/**
 * 已由 CmsUserRole enum + cms_user_role @ElementCollection 取代。
 * 保留此類別避免舊版編譯錯誤，不對應任何資料表。
 */
@Deprecated
public class CmsRole {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
