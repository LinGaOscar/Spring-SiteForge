package com.siteforge.cms.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 儲存服務介面，定義檔案上傳的核心行為
 */
public interface StorageService {
    /**
     * 儲存檔案到儲存介質（本地硬碟或雲端）
     * @param file 上傳的檔案
     * @return 儲存結果，包含原始檔名、目標路徑、MIME 類型、檔案大小
     */
    StorageResult store(MultipartFile file);
}
