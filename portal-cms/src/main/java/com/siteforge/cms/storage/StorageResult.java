package com.siteforge.cms.storage;

/**
 * 檔案儲存操作的結果，封裝上傳後的檔案元數據
 * @param originalFilename 使用者上傳時的原始檔名
 * @param filePath 儲存在系統中的相對路徑（供 Asset 實體引用）
 * @param mimeType 檔案的 MIME 類型（如 image/png）
 * @param size 檔案大小（位元組）
 */
public record StorageResult(
    String originalFilename,
    String filePath,
    String mimeType,
    Long size
) {}
