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

## 常用指令

```bash
# 初始化（首次 clone 或 CI）
./mvnw install -DskipTests

# 啟動（portal-cms 先啟動以執行 Flyway migration）
./mvnw spring-boot:run -pl portal-cms -Dspring-boot.run.profiles=dev
./mvnw spring-boot:run -pl portal-web -Dspring-boot.run.profiles=dev

# 編譯（不啟動）
./mvnw compile -pl portal-domain,portal-cms -am

# 執行測試
./mvnw test -pl portal-cms
./mvnw test -pl portal-domain
./mvnw test -pl portal-cms -Dtest=LocalStorageServiceTest  # 單一測試類
```

Flyway migration 只在 `portal-cms` 啟動時執行，`portal-web` 設定 `spring.flyway.enabled=false`。

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
      → isRwsPage(layoutSet)：RWS 頁面 + 桌面 → redirect:/mobile-required
      → resolveKey()：TemplateKey.name().toLowerCase() → e.g. "rwd_header"
  → Thymeleaf layout/base.html
      th:replace fragments/header/__${headerTemplate}__
      th:replace fragments/body/__${bodyTemplate}__
      th:replace fragments/footer/__${footerTemplate}__
```

- **RWD 模板**（`rwd_*`）：Bootstrap responsive，支援桌面與手機。
- **RWS 模板**（`rws_*`）：手機限定行銷活動頁，桌面連入會跳提示頁。
- `portal.site-id`（application.yml）決定前台對應哪個 Site。

## 角色系統（portal-cms）

`CmsUserRole` enum：`OP`（操作員）、`MA`（主管）、`VI`（檢視者），一人可複合持有。  
Spring Security authority 格式：`ROLE_MA`、`ROLE_OP`、`ROLE_VI`。  
`CmsUser.roles` 以 `@ElementCollection` 儲存於 `cms_user_role(user_id, role)`。

## 主要資料表關係

```
unit (code PK)
cms_user → unit
cms_user_role (user_id, role)

site → page → page_content
            → page_version
page → layout_set (header_key / body_key / footer_key → TemplateKey enum)
page → unit

asset → unit
```

`page.status` 欄位對應 `PageStatus` enum；所有主要表含 `created_at / updated_at / created_by / updated_by` audit 欄位。

## portal-domain 職責

- JPA Entity：`Page`、`LayoutSet`、`PageContent`、`PageVersion`、`Asset`、`CmsUser`、`Unit`
- 共用 enum：`PageStatus`、`TemplateKey`、`CmsUserRole`
- Repository 介面：`PageRepository` 含 `findByUnitCode*` 系列方法用於單位隔離查詢
- `CmsRole` 已廢棄（標記 `@Deprecated`），角色改由 `CmsUserRole` enum 管理

## CMS 後台路由

```
GET  /cms/auth/login        登入頁（Bootstrap 表單）
GET  /cms/dashboard         統計 + MA 待處理列表
GET  /cms/pages             頁面列表（依單位隔離，可依狀態 tab 篩選）
GET  /cms/pages/new         新增頁面（OP 限定）
GET  /cms/pages/{id}/edit   編輯頁面（DRAFT / APPROVED 狀態，OP 限定）
POST /cms/pages/{id}/submit              OP 送審
POST /cms/pages/{id}/approve             MA 放行建立
POST /cms/pages/{id}/reject              MA 退回建立
POST /cms/pages/{id}/request-publish     OP 申請發布
POST /cms/pages/{id}/approve-publish     MA 放行發布
POST /cms/pages/{id}/reject-publish      MA 退回發布
POST /cms/pages/{id}/request-unpublish   OP 申請下架
POST /cms/pages/{id}/confirm-unpublish   MA 確認下架
POST /cms/pages/{id}/unpublish           MA 直接下架
GET  /cms/assets            素材管理（Grid 預覽，全單位可見）
POST /cms/assets/upload     上傳（OP / MA 可操作）
POST /cms/assets/{id}/delete 刪除（同單位 OP / MA 限定）
```

## Flyway Migration 順序

| Version | 內容 |
|---------|------|
| V1 | cms_user、cms_role（已廢棄，V5 重建） |
| V2 | site、layout_set |
| V3 | page、page_content |
| V4 | page_version、asset |
| V5 | unit 表、角色重構（cms_user_role 改為字串）、page/asset/cms_user 加入 unit_code |

新 migration 腳本放在 `portal-cms/src/main/resources/db/migration/`，命名格式：`V{n}__{描述}.sql`。

## 開發注意事項

- Dev seed 帳號設定在 `portal-cms/src/main/resources/application-dev.yml`（`cms.init.admin-*`），`DataInitializer` 只在 `dev` profile 執行，預設帳號屬於 unit `00100`，同時持有 `OP + MA`。
- 固定單位：`00100`、`00800`、`00850`（由 V5 migration 種子資料建立）。
- `CmsUserService.loadUser(username)` 是所有 CMS controller 取得當前使用者（含 unit）的共用入口。
- 新增 TemplateKey 值時必須同步建立對應的 HTML fragment 檔案，否則 Thymeleaf 渲染會拋例外。
- SSO 登入為預留介面，目前僅實作帳密登入。
