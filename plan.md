# SpringSiteForge 建置計畫

## 專案定位

SpringSiteForge 是一套以 Spring Boot 為核心的官網內容管理平台，採前後台分離但不拆微服務的方式建置，分為前台網站 `portal-web`、後台 CMS `portal-cms`，以及共用的 JPA 模組 `portal-domain` 三個 Maven 模組。

兩個 Spring Boot 應用共用同一個 PostgreSQL 資料庫，`portal-web` 唯讀、`portal-cms` 完整讀寫，以資料庫存取層級的職責分離取代服務間 HTTP 呼叫，同時保有 SEO 與 SSR 能力，並把內容維護、發布與版本管理集中在 `portal-cms` 中。

## 建置目標

本計畫要完成一套可上線的 MVP，讓管理者能透過後台建立頁面、編輯內容、指定版型、上傳素材並發布到前台網站。

前台網站只讀取已發布資料，不直接存取後台資料庫；所有資料交換都透過 API 進行，降低耦合並讓後續擴充較容易。

### MVP 定義（SpringSiteForge）

在本計畫中，MVP（Minimum Viable Product, 最小可行產品）指的是「第一版即可上線，滿足基本官網管理需求的最小功能集合」，而不是 Prototype 或一次到位的完整版。

SpringSiteForge 的 MVP 至少要達成：

- 管理者可以登入 `cms-web`。
- 可以透過後台建立 / 編輯 / 刪除頁面與內容區塊。
- 可以為頁面指定 header / body / footer 版型。
- 可以發布與回滾頁面版本。
- `portal-web` 能依 path 正確顯示已發布頁面，並使用對應版型與內容。

進階功能（多語系、複雜工作流程、A/B 測試、完整審計報表）不列入第一版 MVP 範圍，待後續迭代再加入。

## 系統架構

### portal-domain

`portal-domain` 是 Maven 共用模組，不部署為獨立應用，僅作為 `portal-web` 與 `portal-cms` 的依賴項。

主要內容如下：

- JPA Entity（`Page`、`LayoutSet`、`PageContent`、`PageVersion`、`Asset`、`CmsUser` 等）。
- Spring Data JPA Repository 介面。
- 共用 DTO 與常數（如 `PageStatus`、`TemplateKey` 白名單 enum）。

### portal-web

`portal-web` 負責訪客看到的正式網站，使用 Spring Boot MVC 搭配 Thymeleaf 做伺服器端渲染，並透過 Vue.js 增強前端互動體驗。所有資料透過 `portal-domain` 直接查詢共用資料庫（唯讀）。

主要職責如下：

- 接收前台網址請求，透過 `portal-domain` Repository 查詢已發布頁面（status = PUBLISHED）。
- 依設定選擇 header、body、footer 並組裝 Thymeleaf Model，輸出 SEO 友善 HTML。
- 提供唯讀 REST API，供 Vue.js 元件動態載入資料（如搜尋、篩選）。
- 處理 404、500 與快取邏輯。

### portal-cms

`portal-cms` 整合後台管理介面與資料存取邏輯，透過 `portal-domain` 直接讀寫共用資料庫，提供 Thymeleaf 頁面搭配 AJAX REST API 的管理體驗。

主要職責如下：

- 登入與權限控制（Spring Security）。
- 頁面、版型、內容區塊、媒資的 CRUD REST API。
- 驗證 template key 與資料完整性。
- 實作草稿、發布、回滾流程。
- 操作紀錄與版本查詢。

## 核心設計原則

### 1. 前後台分離

`portal-web` 與 `portal-cms` 各自獨立部署，共用同一個 PostgreSQL 資料庫，以職責分離（唯讀 vs 讀寫）取代服務間 HTTP 呼叫，降低網路延遲與運維複雜度。

### 2. 前台只讀已發布版本

`portal-web` 只能查詢 `status = PUBLISHED` 的頁面，此限制由 `portal-domain` 的 Repository 查詢條件強制執行，Service 層禁止傳入草稿資料至 Thymeleaf Model。

### 3. 模板白名單化

後台只能從 `portal-domain` 預先定義的 `TemplateKey` enum 中選擇 header、body、footer，不允許任意輸入模板路徑，避免安全與維運風險。

## 頁面組裝模式

Thymeleaf 支援 fragments 與 layouts，可透過 include 方式將共用區塊與頁面內容組合成完整 HTML；在此架構下，前台頁面將以 `layout/base.html` 為主版型，再動態插入對應的 header、body、footer。

建議第一版先採用較單純的 include-style layout，而非一開始就全面導入複雜的 hierarchical layout，以降低實作與維護成本。

前台渲染流程如下：

1. 使用者進入前台網址，例如 `/about`。
2. `portal-web` PageController 呼叫 Service，透過 `portal-domain` Repository 查詢 path 對應的已發布頁面。
3. Service 組裝 SEO、layout key、section 與 content block 並放入 Model。
4. Thymeleaf 依 layout key 以 `th:replace` 動態插入 header、body、footer fragment 組頁輸出。

## 功能範圍

### 前台功能

- 依 path 顯示頁面。
- 動態套用 header、body、footer。
- 載入 SEO meta 與 Open Graph。
- 顯示錯誤頁與 fallback layout。
- 可加上已發布頁快取機制。

### 後台功能

- 管理者登入。
- 頁面新增、編輯、刪除。
- 版型設定。
- 內容區塊編輯。
- 媒資上傳與管理。
- 草稿儲存。
- 發布與回滾。
- 操作紀錄與版本查詢。

## 資料模型規劃

建議第一版至少建立以下主要資料表：

### site

- 站台代碼。
- 站台名稱。
- 網域。
- 預設語系。
- 啟用狀態。

### page

- page id。
- site id。
- path。
- 頁面名稱。
- SEO title。
- SEO description。
- layout set id。
- status。

### layout_set

- layout id。
- header template key。
- body template key。
- footer template key。
- 描述與啟用狀態。

### page_content

- page id。
- block key。
- sort order。
- content json。
- locale。

### page_version

- version no。
- page id。
- snapshot json。
- draft/published 狀態。
- published at。
- published by。

### asset

- asset id。
- 檔名。
- 檔案路徑或 URL。
- mime type。
- size。
- 建立時間。

### cms_user / cms_role

- 管理者帳號。
- 角色。
- 權限。
- 狀態。
- 最後登入時間。

## API 規劃

### Public REST API（portal-web 提供，唯讀，供 Vue.js 元件使用）

- `GET /api/pages/search`
- `GET /api/pages/{path}`（Vue.js 動態換頁用，非初次 SSR）
- `GET /api/assets/{id}`

### CMS REST API（portal-cms 提供，供後台 Vue.js / AJAX 使用）

- `POST /api/cms/auth/login`
- `GET /api/cms/pages`
- `POST /api/cms/pages`
- `PUT /api/cms/pages/{id}`
- `DELETE /api/cms/pages/{id}`
- `GET /api/cms/layouts`
- `POST /api/cms/layouts`
- `PUT /api/cms/layouts/{id}`
- `POST /api/cms/assets`
- `POST /api/cms/pages/{id}/publish`
- `POST /api/cms/pages/{id}/rollback`
- `GET /api/cms/pages/{id}/versions`

## 專案結構

### portal-domain（共用模組，不部署）

```text
portal-domain
 ├─ entity       (JPA Entity)
 ├─ repository   (Spring Data JPA Repository)
 ├─ enums        (PageStatus, TemplateKey 白名單 enum)
 └─ dto          (跨模組共用 DTO)
```

### portal-web

```text
portal-web
 ├─ controller   (Thymeleaf MVC + 唯讀 REST API)
 ├─ service
 ├─ config
 └─ resources
    ├─ templates
    │  ├─ layout
    │  ├─ fragments
    │  │  ├─ header
    │  │  ├─ footer
    │  │  └─ components
    │  └─ pages
    └─ static
       └─ js     (Vue.js 元件)
```

### portal-cms

```text
portal-cms
 ├─ controller   (Thymeleaf MVC + CMS REST API)
 ├─ service
 ├─ security
 ├─ config
 └─ resources
    ├─ templates
    │  ├─ layout
    │  ├─ dashboard
    │  ├─ page
    │  ├─ layout-config
    │  ├─ content
    │  └─ fragments
    └─ static
       └─ js     (Vue.js 元件)
```

## 技術選型

第一版建議技術如下：

- Java 21
- Spring Boot 3.x
- Spring Web MVC
- Thymeleaf（SSR 頁面殼層）
- Vue.js 3（前端互動元件，CDN 引入或 Vite 打包）
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway（schema 版本管理）
- Redis（可於第二階段補上）
- OpenAPI / Swagger（CMS API 文件）
- MapStruct
- Lombok（視團隊接受度而定）

## 分階段建置時程

### Phase 1：基礎骨架（1.5 週）

- 建立 Maven parent pom，初始化 `portal-domain`、`portal-web`、`portal-cms` 三個模組。
- 建立共用設定與 profile（dev/prod）。
- 引入 Flyway，建立 PostgreSQL schema 初版。
- 完成 `portal-cms` 基本登入（Spring Security）與錯誤處理。
- 定義統一 API response format（`{ success, data, error }`）。

### Phase 2：portal-cms 核心（2 週）

- 完成 `portal-domain` entity 與 repository。
- 完成 page/layout/content/asset CRUD REST API。
- 完成角色權限控制。
- 完成 draft/published 資料模型。
- 完成發布與回滾 service。

### Phase 3：portal-cms 後台頁面（1.5 週）

- Dashboard。
- 頁面列表與查詢（Thymeleaf + AJAX）。
- 頁面建立與編輯。
- 版型設定頁。
- 內容區塊編輯頁。
- 發布紀錄與版本查詢頁。
- 媒資管理頁。

### Phase 4：portal-web 前台渲染（2 週）

- 依 path 透過 `portal-domain` Repository 查詢已發布頁面。
- 動態載入 header、body、footer fragment。
- 載入 SEO meta 與 Open Graph。
- 建立 404、500、maintenance 頁。
- 加入 fallback layout。

### Phase 5：預覽與優化（1 至 2 週）

- 草稿預覽。
- 已發布頁快取。
- 操作審計 log。
- 檔案命名與媒資規則。
- SEO 欄位強化。
- 基本監控與告警。

## 本機開發啟動流程

本機開發時預設三個 Spring Boot 應用會同時啟動，並各自使用不同的 HTTP port 以避免衝突。

### 預設 Port 與 URL

- `portal-web`：`http://localhost:8100`
- `portal-cms`：`http://localhost:8200`

各應用在 `application.yml` 中設定 `server.port`，例如：

```yaml
# portal-web
server:
  port: 8100

# portal-cms
server:
  port: 8200
```

### 啟動順序建議

1. 確認 PostgreSQL 已啟動並套用 Flyway migration。
2. 啟動 `portal-cms`（8200）：透過瀏覽器登入後台確認 DB 連線正常。
3. 啟動 `portal-web`（8100）：確認前台能查詢已發布頁面並正確渲染。

兩個應用可獨立啟動，`portal-web` 不依賴 `portal-cms` 服務。

## 驗收標準

第一版驗收至少應達成以下目標：

- 管理者可以登入 `cms-web`。
- 可以建立頁面並指定 layout。
- 可以編輯內容區塊並儲存草稿。
- 可以發布頁面並回滾版本。
- `portal-web` 可依 path 顯示對應正式頁面。
- header/body/footer 能依設定正確切換。
- 有基本 SEO、404 與 500 頁面。

## 風險與注意事項

- 不建議混用 JSP 與 Thymeleaf，會提高維護成本與模板複雜度。
- `portal-web` 只能以唯讀方式存取共用資料庫，禁止在前台 Service 層執行任何寫入操作。
- 後台不可讓使用者任意輸入模板路徑，模板必須白名單化。
- 發布資料應以版本快照保存，避免直接覆蓋正式內容。
- 第一版避免過度設計，先完成可用 MVP，再補預覽、快取、審計與多語功能。

## 命名定案

本計畫名稱定為 **SpringSiteForge**，系統名稱如下：

- 前台網站：`portal-web`
- 後台 CMS：`portal-cms`
- 共用 JPA 模組：`portal-domain`（Maven 模組，不部署）

此命名方式可同時保留業務語意與工程可讀性，適合作為 repository、artifact 與部署單位名稱。
