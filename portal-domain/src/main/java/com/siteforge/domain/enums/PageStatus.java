package com.siteforge.domain.enums;

public enum PageStatus {
    /** OP 建立或被退回後的編輯中狀態 */
    DRAFT,
    /** OP 送審，等待 MA 放行 */
    PENDING_REVIEW,
    /** MA 放行，可申請發布 */
    APPROVED,
    /** OP 申請發布，等待 MA 放行 */
    PENDING_PUBLISH,
    /** 已發布，對前台可見 */
    PUBLISHED
}
