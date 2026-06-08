# 元件圖片載入策略

## 現況（Method B）

portal-web 的 body 元件（如 `rwd_text_image`）使用 `imageUrl` 欄位（type: `text`）。
CMS 編輯者在 content_json 中填入完整 URL，Thymeleaf 直接渲染：

```json
{
  "imageUrl": "http://localhost:8200/cms/uploads/about/company.png"
}
```

**優點：** 不需額外 DB 查詢，portal-web 保持純唯讀 SSR。  
**限制：** URL 包含 host，跨環境（dev/prod）需手動替換；無法與 CMS asset 管理系統整合。

---

## 未來擴展（Method C）：asset_ids + AssetResolver

當 portal-web 需要支援從 CMS Asset 管理介面選圖（透過 `asset_ids` 欄位），採以下方案：

### 1. Schema 欄位改為 asset_ids

```json
{
  "name": "imageId",
  "type": "asset_ids",
  "label": "主圖（從素材庫選取）",
  "default": []
}
```

### 2. PageRenderService 加入 AssetResolver

在 `portal-web/src/main/java/com/siteforge/web/service/PageRenderService.java` 的 `buildContentViews()` 中，
於組裝 `PageContentView` 前，對每個 block 的 contentMap 執行 ID → URL 替換：

```java
// 注入 AssetRepository（portal-domain 已定義）
private final AssetRepository assetRepository;

private Map<String, Object> resolveAssetIds(Map<String, Object> contentMap) {
    // 找出值為 List<Number> 的欄位，逐一查 DB 取 file_path，替換成 URL
    Map<String, Object> resolved = new LinkedHashMap<>(contentMap);
    contentMap.forEach((key, value) -> {
        if (value instanceof List<?> ids && !ids.isEmpty() && ids.get(0) instanceof Number) {
            List<Long> assetIds = ids.stream().map(id -> ((Number) id).longValue()).toList();
            // 只取第一張（單圖欄位）
            assetRepository.findById(assetIds.get(0))
                .ifPresent(asset -> resolved.put(key + "Url", baseUrl + asset.getFilePath()));
        }
    });
    return resolved;
}
```

**規則：** `imageId`（asset_ids）→ 解析後在 contentMap 多放一個 `imageIdUrl` 鍵，
Thymeleaf fragment 改用 `${config['imageIdUrl'] ?: ''}` 讀取。

### 3. baseUrl 來源

`portal-web/src/main/resources/application-dev.yml` 新增：

```yaml
portal:
  asset-base-url: http://localhost:8200/cms
```

正式環境（`application-prod.yml`）填入對應 CDN 或主機位址。

### 4. 優點

- 圖片 URL 不 hardcode 在 content_json，跨環境自動切換。
- 與 CMS 素材管理整合，編輯者可從 Asset Grid 選圖。
- portal-web 仍是 SSR 唯讀，僅多一次 asset 資料表查詢。

### 5. 遷移路徑

1. `rwd_text_image.html` schema 新增 `imageId`（type: `asset_ids`）欄位，保留舊 `imageUrl` 欄位向後相容。
2. 實作 `AssetResolver` service，在 `buildContentViews()` 後處理。
3. 舊資料（`imageUrl` 有值）繼續走 Method B 路徑；新資料走 Method C 路徑。
4. 待全部頁面遷移完成後，移除 `imageUrl` 欄位。
