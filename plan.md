# SpringSiteForge 建置計畫

## 專案定位

SpringSiteForge 是一套以 Spring Boot 為核心的官網內容管理平台，採前後台分離但不拆微服務的方式建置，分為前台網站 `portal-web`、後台控制頁面 `cms-web`、以及後台 API `cms-api` 三個應用。

此架構的目標是讓前台保有 SEO 與 SSR 能力，並由後台決定每個頁面要套用哪一組 header、body、footer 模板，同時把內容維護、發布與版本管理集中在 CMS 流程中。

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

### portal-web

`portal-web` 負責訪客看到的正式網站，使用 Spring Boot MVC 搭配 Thymeleaf 做伺服器端渲染，並依頁面設定動態組裝 fragments 與 layouts。

主要職責如下：

- 接收前台網址請求。
- 呼叫 `cms-api` 取得已發布頁面。
- 依設定選擇 header、body、footer。
- 輸出 SEO 友善 HTML。
- 處理 404、500 與快取邏輯。

### cms-web

`cms-web` 是後台控制頁面，提供管理者登入後操作頁面、版型、內容、發布、媒資與版本等功能；此層屬於管理介面，不直接承擔核心商業邏輯。

主要職責如下：

- 登入與權限控制頁面。
- 頁面 CRUD 與查詢。
- 版型綁定與內容編輯。
- 預覽、發布與回滾操作。
- 媒資管理與操作紀錄查詢。

### cms-api

`cms-api` 是系統的資料與流程中心，負責提供 public/admin API、執行驗證、存取資料庫、維護發布流程與版本管理。

主要職責如下：

- 提供前台與後台使用的 REST API。
- 管理頁面、版型、內容區塊與資產。
- 驗證 template key 與資料完整性。
- 實作草稿、發布、回滾流程。
- 控制權限與審計紀錄。

## 核心設計原則

### 1. 前後台分離

前台網站、後台頁面、後台 API 各自獨立部署，避免把控制頁面、商業邏輯與正式網站渲染綁在同一個應用中。

### 2. API 為唯一資料入口

`portal-web` 與 `cms-web` 都只能透過 `cms-api` 交換資料，不直接讀寫資料庫，以維持單一資料治理邊界。

### 3. 前台只讀已發布版本

前台正式站不得讀取草稿資料，所有正式內容都來自已發布版本，避免編輯中內容外漏。

### 4. 模板白名單化

後台只能從系統預先定義的模板清單中選擇 header、body、footer，不允許任意輸入模板路徑，避免安全與維運風險。

## 頁面組裝模式

Thymeleaf 支援 fragments 與 layouts，可透過 include 方式將共用區塊與頁面內容組合成完整 HTML；在此架構下，前台頁面將以 `layout/base.html` 為主版型，再動態插入對應的 header、body、footer。

建議第一版先採用較單純的 include-style layout，而非一開始就全面導入複雜的 hierarchical layout，以降低實作與維護成本。

前台渲染流程如下：

1. 使用者進入前台網址，例如 `/about`。
2. `portal-web` 呼叫 `cms-api` 查詢對應 path 的已發布頁面。
3. API 回傳 SEO、layout、section 與 content block。
4. `portal-web` 將模板 key 放入 model。
5. Thymeleaf 依設定以 `th:replace` 或 fragment expression 組頁輸出。

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

### admin_user / admin_role

- 管理者帳號。
- 角色。
- 權限。
- 狀態。
- 最後登入時間。

## API 規劃

### Public API

前台使用的 API 建議如下：

- `GET /api/public/pages/{path}`
- `GET /api/public/sites/{siteCode}`
- `GET /api/public/assets/{id}`

`GET /api/public/pages/{path}` 建議回傳：

- `pagePath`
- `seo`
- `layout`
- `sections`
- `contentBlocks`
- `publishVersion`

### Admin API

後台使用的 API 建議如下：

- `POST /api/admin/auth/login`
- `GET /api/admin/pages`
- `POST /api/admin/pages`
- `PUT /api/admin/pages/{id}`
- `GET /api/admin/layouts`
- `POST /api/admin/layouts`
- `POST /api/admin/assets`
- `POST /api/admin/pages/{id}/publish`
- `POST /api/admin/pages/{id}/rollback`

## 專案結構

### portal-web

```text
portal-web
 ├─ controller
 ├─ service
 ├─ client
 ├─ model
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
```

### cms-web

```text
cms-web
 ├─ controller
 ├─ service
 ├─ client
 ├─ model
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
```

### cms-api

```text
cms-api
 ├─ controller
 ├─ service
 ├─ repository
 ├─ domain
 ├─ dto
 ├─ security
 └─ config
```

## 技術選型

第一版建議技術如下：

- Java 21  
- Spring Boot 3.x  
- Spring Web MVC  
- Thymeleaf  
- Spring Security  
- Spring Data JPA  
- PostgreSQL  
- Redis（可於第二階段補上）  
- OpenAPI / Swagger  
- MapStruct  
- Lombok（視團隊接受度而定）

## 分階段建置時程

### Phase 1：基礎骨架（1 週）

- 建立 `portal-web`、`cms-web`、`cms-api` 三個專案。
- 建立共用設定與 profile。
- 建立 PostgreSQL schema 初版。
- 完成基本登入與錯誤處理。
- 建立 API response format。

### Phase 2：cms-api 核心（2 週）

- 完成 page/layout/content/asset CRUD。
- 完成角色權限控制。
- 完成 draft/published 資料模型。
- 完成發布與回滾 service。
- 建立 public/admin API。

### Phase 3：cms-web 後台頁面（2 週）

- Dashboard。
- 頁面列表與查詢。
- 頁面建立與編輯。
- 版型設定頁。
- 內容編輯頁。
- 發布紀錄頁。
- 媒資管理頁。

### Phase 4：portal-web 前台渲染（2 週）

- 依 path 查詢已發布頁面。
- 動態載入 header、body、footer。
- 載入 SEO meta。
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
- `cms-web`：`http://localhost:8200`
- `cms-api`：`http://localhost:8300`

各應用在 `application.yml` 中設定 `server.port`，例如：

```yaml
# portal-web
server:
  port: 8100

# cms-web
server:
  port: 8200

# cms-api
server:
  port: 8300
```

Spring Boot 內建的嵌入式伺服器預設為 8080，只要在設定檔覆寫 `server.port` 即可改為其他 port。

### 啟動順序建議

1. 啟動 `cms-api`（8300）：確保 API 與資料庫連線正常。
2. 啟動 `cms-web`（8200）：透過瀏覽器登入後台確認可以呼叫 `cms-api` 正常運作。
3. 啟動 `portal-web`（8100）：確認前台可透過 `cms-api` 取得已發布頁面並渲染。

如需暫時只開發前台或後台，也可以單獨啟動 `cms-api` 搭配其中一個 web 應用使用。

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
- 前台不得直接存取 CMS 資料庫，必須透過 API。
- 後台不可讓使用者任意輸入模板路徑，模板必須白名單化。
- 發布資料應以版本快照保存，避免直接覆蓋正式內容。
- 第一版避免過度設計，先完成可用 MVP，再補預覽、快取、審計與多語功能。

## 命名定案

本計畫名稱定為 **SpringSiteForge**，系統名稱如下：

- 前台網站：`portal-web`
- 後台控制頁面：`cms-web`
- 後台 API：`cms-api`

此命名方式可同時保留業務語意與工程可讀性，適合作為 repository、artifact 與部署單位名稱。
