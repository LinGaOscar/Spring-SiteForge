# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案概述

**SpringSiteForge** 是以 Spring Boot 為核心的官網 CMS 平台，採 Maven multi-module 結構：

| 模組 | 職責 | Port |
|------|------|------|
| `portal-domain` | JPA entities、repositories、共用 enum/DTO（不部署） | — |
| `portal-web` | 前台 SSR 網站（Thymeleaf + Vue.js + 唯讀 REST API） | 8100 |
| `portal-cms` | 後台 CMS（Thymeleaf + Vue.js + CMS REST API，完整讀寫） | 8200 |

兩個 Spring Boot 應用共用同一個 PostgreSQL 資料庫，透過 `portal-domain` 存取。

## 技術棧

- Java 21 + Spring Boot 3.x
- Spring Web MVC + Thymeleaf（SSR 頁面殼層）
- Vue.js 3（前端互動元件，CDN 引入或 Vite 打包）
- Spring Security（portal-cms 登入與權限）
- Spring Data JPA + PostgreSQL + Flyway
- MapStruct + Lombok
- OpenAPI / Swagger（CMS API 文件）

## 本機啟動順序

```bash
# 1. 確認 PostgreSQL 啟動並套用 Flyway migration（portal-cms 啟動時自動執行）
./mvnw spring-boot:run -pl portal-cms

# 2. 啟動前台
./mvnw spring-boot:run -pl portal-web
```

兩個應用可獨立啟動，`portal-web` 不依賴 `portal-cms` 服務。

## 架構核心原則

1. **共用 DB，職責分離**：`portal-web` 唯讀、`portal-cms` 完整讀寫，限制在 Service 層查詢條件強制執行，不依賴 API 權限。
2. **前台只讀已發布版本**：`portal-web` 只能查詢 `status = PUBLISHED`，禁止在前台 Service 層執行任何寫入操作。
3. **模板白名單化**：header/body/footer 模板 key 只能從 `portal-domain` 的 `TemplateKey` enum 中選擇，後台不允許使用者任意輸入模板路徑。
4. **版本快照**：發布時以 JSON snapshot 保存完整內容至 `page_version`，回滾不覆蓋原始資料。

## 頁面組裝流程（portal-web）

```
/about → PageController → Service → portal-domain Repository
       ← Page(status=PUBLISHED) + LayoutSet + PageContent
       → Thymeleaf layout/base.html
          th:replace fragments/header/{headerKey}
          th:replace fragments/body/{bodyKey}
          th:replace fragments/footer/{footerKey}
       + Vue.js 元件掛載（互動功能）
```

第一版採用 include-style layout（`th:replace`），不使用複雜的 hierarchical layout。

## API 路由規範

```
# Public（portal-web，唯讀，供 Vue.js 使用）
GET  /api/pages/search
GET  /api/pages/{path}
GET  /api/assets/{id}

# CMS（portal-cms，需 JWT）
POST /api/cms/auth/login
GET  /api/cms/pages
POST /api/cms/pages
PUT  /api/cms/pages/{id}
DELETE /api/cms/pages/{id}
POST /api/cms/pages/{id}/publish
POST /api/cms/pages/{id}/rollback
GET  /api/cms/pages/{id}/versions
GET  /api/cms/layouts
POST /api/cms/layouts
PUT  /api/cms/layouts/{id}
POST /api/cms/assets
```

## 主要資料表

`site` → `page`（含 SEO、status、layout_set_id）→ `page_content`（block key + content JSON）→ `page_version`（snapshot JSON、draft/published）
`layout_set`（header/body/footer template key）
`asset`（檔案路徑或 URL、mime type）
`cms_user` / `cms_role`

所有主要資料表含 `created_at`、`updated_at`、`created_by` audit 欄位。

## portal-domain 職責

- JPA Entity（`Page`、`LayoutSet`、`PageContent`、`PageVersion`、`Asset`、`CmsUser` 等）
- Spring Data JPA Repository 介面
- 共用 enum：`PageStatus`（DRAFT / PUBLISHED）、`TemplateKey`（模板白名單）
- 跨模組共用 DTO

## 開發注意事項

- Flyway migration 腳本放在 `portal-cms/src/main/resources/db/migration/`，`portal-web` 設定 `spring.flyway.enabled=false`。
- 不混用 JSP 與 Thymeleaf。
- 第一版 MVP 不包含多語系、A/B 測試、複雜審計報表；Redis 快取列為第二階段。
- 新增功能前先確認是否在 MVP 範圍內（見 `plan.md`）。
