# SpringSiteForge

以 Spring Boot 為核心的企業官網 CMS 平台。採 Maven multi-module 結構，前台與後台共用同一個 PostgreSQL 資料庫，職責嚴格分離。

---

## 專案架構

```
spring-siteforge/
├── portal-domain      # JPA Entity、Repository、共用 Enum（不部署，僅作為依賴）
├── portal-web         # 前台 SSR 網站（port 8100）
└── portal-cms         # 後台 CMS 管理介面（port 8200）
```

| 模組 | 職責 | Port |
|------|------|------|
| `portal-domain` | Entity、Repository、Enum/DTO 共用層 | — |
| `portal-web` | 前台 Thymeleaf SSR + Vue.js，唯讀已發布頁面 | 8100 |
| `portal-cms` | 後台 CMS REST API + 管理介面，需 JWT 驗證 | 8200 |

### 核心原則

- **portal-web 唯讀**：只查詢 `status = PUBLISHED` 的頁面，Service 層禁止寫入
- **模板白名單**：Header/Body/Footer 模板 key 只能從 `TemplateKey` enum 選擇
- **版本快照**：發布時以 JSON 完整備份至 `page_version`，回滾不覆蓋原始資料
- **企業靜態資源統一由 CMS 提供**：上傳圖片經 `/uploads/**` 從 portal-cms 取得

---

## 技術棧

- Java 21 + Spring Boot 3.3
- Spring Data JPA + PostgreSQL + Flyway
- Spring Security（portal-cms JWT Session 驗證）
- Thymeleaf（SSR 頁面殼層）+ Vue.js 3（互動元件）
- Redis（portal-web 頁面快取）
- Lombok + MapStruct

---

## 資料庫 Schema

詳細資料表定義請參閱 [docs/database.md](docs/database.md)。

```
site
 └── page
      ├── page_content
      └── page_version

layout_set ←── page
asset
cms_user ── cms_role
```

---

## API 路由

完整 API 文件請參閱 [docs/api.md](docs/api.md)。

| 分類 | 前綴 | 應用 |
|------|------|------|
| 前台 Public | `/api/` | portal-web (8100) |
| CMS | `/api/cms/` | portal-cms (8200) |

---

## 頁面組裝流程（portal-web）

```
Request /about
  → PageController
  → PageService（查詢 status=PUBLISHED）
  → Page + LayoutSet + PageContent
  → Thymeleaf layout/base.html
       th:replace fragments/header/{headerKey}
       th:replace fragments/body/{bodyKey}
       th:replace fragments/footer/{footerKey}
  + Vue.js 元件掛載
```

---

## 本機開發環境

### 前置需求

- Java 21
- Maven 3.9+
- Docker（用於啟動 PostgreSQL + Redis）

### 1. 啟動基礎設施

```bash
# 複製環境變數範本並填入密碼
cp .env.example .env

# 啟動 PostgreSQL + Redis
docker compose up -d
```

預設連線資訊（`.env.example` 預設值）：

| 項目 | 值 |
|------|-----|
| PostgreSQL host | `localhost:5432` |
| Database | `siteforge_db` |
| Username | `siteforge` |
| Redis host | `localhost:6379` |

### 2. 啟動 portal-cms（後台 + Flyway migration）

```bash
mvn spring-boot:run -pl portal-cms -Dspring-boot.run.profiles=dev
```

首次啟動會自動執行 Flyway migration 建立所有資料表，並透過 `DataInitializer` 建立預設管理員帳號。

### 3. 啟動 portal-web（前台）

```bash
mvn spring-boot:run -pl portal-web -Dspring-boot.run.profiles=dev
```

兩個應用可獨立啟動，`portal-web` 不依賴 `portal-cms` 服務。

### 存取位址

| 應用 | URL |
|------|-----|
| 前台 | http://localhost:8100 |
| 後台 CMS | http://localhost:8200 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

> **Dev 環境注意**：頁面模板中的圖片 URL 前綴需加 `http://localhost:8200`（已設定於 `cms.asset-base-url`），因為上傳圖片統一由 portal-cms 的 `/uploads/**` 提供。

---

## 執行測試

```bash
# 全模組
mvn test

# 單一模組
mvn test -pl portal-cms
mvn test -pl portal-domain
```

---

## 模組依賴關係

```
portal-web  ──┐
               ├──→ portal-domain ──→ PostgreSQL
portal-cms  ──┘
    │
    └──→ Redis（portal-web 快取）
```
