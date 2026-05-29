# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案概述

**SpringSiteForge** 是以 Spring Boot 為核心的企業官網 CMS 平台，採 Maven multi-module 結構：

| 模組 | 職責 | Port |
|------|------|------|
| `portal-domain` | JPA entities、repositories、共用 enum（不部署） | — |
| `portal-web` | 前台 SSR 網站（Thymeleaf，唯讀） | 8100 |
| `portal-cms` | 後台 CMS（Thymeleaf + Spring Security，完整讀寫） | 8200 |

兩個 Spring Boot 應用共用同一個 PostgreSQL 資料庫，透過 `portal-domain` 存取。

## 開發環境啟動流程

```bash
# 1. 建立 DB（首次，自動執行 db/init/*.sql）
docker compose up -d

# 2. 重置 DB（清除所有資料重來）
docker compose down -v && docker compose up -d

# 3. Maven install（首次或 portal-domain 有異動時）
./mvnw install -DskipTests

# 4. 啟動應用（portal-web 先啟動，ComponentSyncRunner 掃描完元件後 CMS 選單才有資料）
./mvnw spring-boot:run -pl portal-web -Dspring-boot.run.profiles=dev   # http://localhost:8100/web/
./mvnw spring-boot:run -pl portal-cms -Dspring-boot.run.profiles=dev   # http://localhost:8200/cws/

# 5. 測試（commit 前必跑全套）
./mvnw test -pl portal-web,portal-cms,portal-domain
./mvnw test -pl portal-cms -Dtest=LocalStorageServiceTest               # 單一測試類
```

## 架構核心原則

1. **共用 DB，職責分離**：`portal-web` 只能查詢 `status = PUBLISHED`，Service 層強制執行，不依賴 API 權限。
2. **模板白名單**：header/body/footer 模板 key 只能來自 `TemplateKey` enum（`portal-domain`），後台不允許任意輸入路徑。
3. **單位隔離**：CMS 使用者屬於固定 5 碼單位（`unit.code`，如 `00100`），查詢和操作只限本單位頁面。Assets 全單位可瀏覽，但只有同單位才能刪除。
4. **版本快照**：發布時以 JSON snapshot 保存完整內容至 `page_version`。

## 頁面狀態機（portal-cms）

```
DRAFT → [OP 送審] → PENDING_REVIEW → [MA 放行] → APPROVED
                                   ↑ [MA 退回]
APPROVED → [OP 申請發布] → PENDING_PUBLISH → [MA 放行] → PUBLISHED
                                           ↑ [MA 退回]
PUBLISHED → [OP 申請下架] → PENDING_UNPUBLISH → [MA 確認] → APPROVED
PUBLISHED → [MA 直接下架] → APPROVED
```

**自審防護**：`page.submitted_by == currentUser.username` 時，MA 無法審核（`WorkflowService` 強制檢查）。

## 前台頁面組裝流程（portal-web）

```
請求 /{*path}
  → DeviceInterceptor：Cookie(view_mode) > User-Agent → isMobile
  → PageController.renderPage()
      → findPublishedPage(siteId, path)：找不到 → redirect:/
      → buildContentViews(pageId)：從 page_content 依 sort_order 組出 List<PageContentView>
      → isRwsPage(layoutSet, contents)：layoutSet header/footer key 以 RWS_ 開頭，
                                        或任一 block_key 以 RWS_ 開頭 → 桌面 redirect:/mobile-required
      → buildModel()：
          headerTemplate = layoutSet.headerKey.name().toLowerCase()
          footerTemplate = layoutSet.footerKey.name().toLowerCase()
          headerConfig   = parseConfig(page.headerConfigJson)  → Map<String,Object>
          footerConfig   = parseConfig(page.footerConfigJson)  → Map<String,Object>
  → Thymeleaf layout/base.html
      th:replace fragments/header/__${headerTemplate}__ :: header(config=${headerConfig})
      th:each block : ${pageContents}
          th:replace fragments/body/__${block.blockKey}__ :: body(config=${block.contentMap})
      th:replace fragments/footer/__${footerTemplate}__ :: footer(config=${footerConfig})
```

- **RWD 模板**（`rwd_*`）：Bootstrap responsive，支援桌面與手機。
- **RWS 模板**（`rws_*`）：手機限定行銷活動頁，桌面連入會跳提示頁。
- **body_only_mobile**：RWS 元件，client-side JS 偵測桌面後 alert + redirect 首頁（server-side 防護留待 Plan 2）。
- `portal.site-id`（application.yml）決定前台對應哪個 Site。
- `PageContentView` record：`(String blockKey, int sortOrder, Map<String,Object> contentMap)`，contentMap 由 `page_content.content_json`（Jackson）解析。

## 角色系統（portal-cms）

`CmsUserRole` enum：`OP`（操作員）、`MA`（主管）、`VI`（檢視者），一人可複合持有。  
Spring Security authority 格式：`ROLE_MA`、`ROLE_OP`、`ROLE_VI`。  
`CmsUser.roles` 以 `@ElementCollection` 儲存於 `cms_user_role(user_id, role)`。

## 主要資料表關係

```
unit (code PK)
cms_user → unit
cms_user_role (user_id, role)

site → page → page_content (block_key, sort_order, content_json, locale)
            → page_version
page → layout_set (header_key / body_key / footer_key → TemplateKey enum)
page → unit
page.header_config_json / footer_config_json  ← 各頁獨立的 header/footer 客製化 JSON

component_definition (key PK, type BODY/HEADER/FOOTER, device_mode RWD/RWS,
                      schema_json, active, synced_at)

asset → unit
```

`page.status` 欄位對應 `PageStatus` enum；所有主要表含 `created_at / updated_at / created_by / updated_by` audit 欄位。

`component_definition` 由 `ComponentSyncRunner` 在 portal-web 啟動時自動同步，掃描 `templates/fragments/{body,header,footer}/*.html`，並解析 HTML 內嵌的 `<!--@component-schema...@end-component-schema-->` 注釋取得 `schema_json` 與 `device_mode`。

## portal-domain 職責

- JPA Entity：`Page`、`LayoutSet`、`PageContent`、`PageVersion`、`Asset`、`CmsUser`、`Unit`
- 共用 enum：`PageStatus`、`TemplateKey`、`CmsUserRole`
- Repository 介面：`PageRepository` 含 `findByUnitCode*` 系列方法用於單位隔離查詢

## CMS 後台路由

context-path 為 `/cws`，所有路由以此為根（`http://localhost:8200/cws/...`）。

```
GET  /cws/auth/login        登入頁（Bootstrap 表單）
GET  /cws/dashboard         統計 + MA 待處理列表
GET  /cws/pages             頁面列表（依單位隔離，可依狀態 tab 篩選）
GET  /cws/pages/new         新增頁面（OP 限定）
GET  /cws/pages/{id}/edit   編輯頁面（DRAFT / APPROVED 狀態，OP 限定）
POST /cws/pages/{id}/submit              OP 送審
POST /cws/pages/{id}/approve             MA 放行建立
POST /cws/pages/{id}/reject              MA 退回建立
POST /cws/pages/{id}/request-publish     OP 申請發布
POST /cws/pages/{id}/approve-publish     MA 放行發布
POST /cws/pages/{id}/reject-publish      MA 退回發布
POST /cws/pages/{id}/request-unpublish   OP 申請下架
POST /cws/pages/{id}/confirm-unpublish   MA 確認下架
POST /cws/pages/{id}/unpublish           MA 直接下架
GET  /cws/components                        元件管理列表（component_definition）
GET  /cws/pages/{id}/content               頁面 body 區塊管理（新增/編輯/刪除/排序）
POST /cws/pages/{id}/content               新增 body block（block_key + sort_order + content_json）
POST /cws/pages/{id}/content/{cid}        更新 body block content_json
POST /cws/pages/{id}/content/{cid}/delete 刪除 body block
GET  /cws/assets            素材管理（Grid 預覽，全單位可見）
POST /cws/assets/upload     上傳（OP / MA 可操作）
POST /cws/assets/{id}/delete 刪除（同單位 OP / MA 限定）
```

## DB Schema 管理

Schema 和種子資料由 Docker Compose 在**首次建立容器**時執行，與應用完全解耦：

| 檔案 | 內容 |
|------|------|
| `db/init/01_schema.sql` | 完整 DDL（所有資料表） |
| `db/init/02_seed.sql` | 種子資料（unit、admin 帳號、site、layout_set、預設頁面） |

Schema 變更直接修改 `01_schema.sql`，重置 DB 用 `docker compose down -v && docker compose up -d`。

## 元件開發流程（新增元件 → CMS 發布）

### Step 1：在 portal-web 建立 fragment

於 `portal-web/src/main/resources/templates/fragments/body/` 建立 HTML 檔，依慣例命名（`rwd_` 開頭為 RWD，`rws_` 開頭或 `body_only_mobile` 為 RWS）：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<!--@component-schema
{
  "deviceMode": "RWD",
  "fields": [
    {"name": "title",   "type": "text",      "label": "標題",   "default": "預設標題"},
    {"name": "imageId", "type": "asset_ids", "label": "主圖",   "default": []},
    {"name": "body",    "type": "richtext",  "label": "內文",   "default": ""}
  ]
}
@end-component-schema-->
<div th:fragment="body(config)">
  <!-- 目前硬編碼展示；Plan 2 改為讀取 config 值渲染 -->
</div>
</body>
</html>
```

**欄位 type 清單：** `text`、`links`（label+url 陣列）、`asset_ids`（素材 ID 陣列）、`number`、`richtext`

header/footer 同理，fragment 宣告改為 `th:fragment="header(config)"` / `th:fragment="footer(config)"`。

### Step 2：重啟 portal-web，確認元件已同步

```bash
./mvnw spring-boot:run -pl portal-web -Dspring-boot.run.profiles=dev
```

`ComponentSyncRunner` 啟動時自動掃描所有 fragment，將元件寫入 `component_definition`（含 `schema_json`、`device_mode`）。可查 `/cws/components` 確認新元件出現。

### Step 3：CMS 建立頁面並加入元件

1. OP 登入 CMS → `/cws/pages/new`，填入路徑、選擇 header/footer template（TemplateKey 白名單）
2. 編輯頁面，加入 body 元件（選擇 `block_key`，寫入 `page_content`）
3. （Plan 2）填寫各欄位 config，存至 `page_content.content_json`
4. OP 送審 → MA 放行 → APPROVED
5. OP 申請發布 → MA 放行發布 → **PUBLISHED**

### Step 4：portal-web 渲染

`PageController` 從 DB 取出 `page_content` 組成 `List<PageContentView>`，各 block 的 `contentMap`（JSON 解析）傳入 fragment 的 `config` 參數。

---

## portal-web 其他路由

context-path 為 `/web`，所有路由以此為根（`http://localhost:8100/web/...`）。

```
GET /web/mobile-required          桌面裝置開啟 RWS 頁面時的提示頁
GET /web/switch-view              手動切換桌機/手機視圖（寫入 view_mode Cookie，redirect 回 Referer）
GET /web/preview/component/{key}  dev 環境：依 block_key 渲染單一 body 元件預覽（BODY 類型限定）
GET /web/preview/{pageId}         dev 環境：渲染任意 page（含未發布），headerConfig/footerConfig 為空 Map
```

素材靜態資源由 portal-cms 提供：`http://localhost:8200/cws/uploads/**`，portal-web 的 template 直接引用此 URL。  
`cms.preview-base-url`（dev）已設為 `http://localhost:8100/web`，包含 context-path，CMS 的「↗ 預覽」連結才能正確指向 portal-web。

---

## 開發注意事項

- Dev 管理員帳號（`manager` / `siteforge2026`）由 `db/init/02_seed.sql` 建立，使用 pgcrypto bcrypt，與 Spring Security 相容。
- 固定單位：`00100`、`00800`、`00850`。
- `CmsUserService.loadUser(username)` 是所有 CMS controller 取得當前使用者（含 unit）的共用入口。
- 新增 TemplateKey 值時必須同步建立對應的 HTML fragment 檔案，否則 Thymeleaf 渲染會拋例外。
- 新增 fragment 後**不需要**手動建任何 JSON 檔，schema 內嵌在 HTML 注釋中，重啟 portal-web 即自動同步。
- `cms.preview-base-url` 僅設定於 `application-dev.yml`，正式環境不存在 → CMS 的「↗ 預覽」按鈕與元件 iframe 為 dev only 功能。
- `GlobalModelAdvice` 替所有 CMS Thymeleaf template 注入 `currentUri`（當前請求 URI），供 sidebar 高亮判斷使用。
- SSO 登入為預留介面，目前僅實作帳密登入。
