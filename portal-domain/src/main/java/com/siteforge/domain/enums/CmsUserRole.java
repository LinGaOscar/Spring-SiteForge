package com.siteforge.domain.enums;

public enum CmsUserRole {
    /** 操作員：建立頁面、編輯、送審、申請發布 */
    OP,
    /** 主管：放行 / 退回（不可審核自己送出的） */
    MA,
    /** 檢視者：唯讀 */
    VI
}
