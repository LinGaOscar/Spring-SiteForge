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
- **模板白名單**：Header/Footer key 來自 `TemplateKey` enum；Body key 由 `ComponentSyncRunner` 在 portal-web 啟動時掃描 fragment 檔自動同步至 `component_definition` 表
- **版本快照**：發布時以 JSON 完整備份至 `page_version`，回滾不覆蓋原始資料
- **企業靜態資源統一由 CMS 提供**：上傳圖片經 `/uploads/**` 從 portal-cms 取得

---

## 技術棧

- Java 21 + Spring Boot 3.3
- Spring Data JPA + PostgreSQL（Schema 由 `db/init/*.sql` 管理，無 Flyway）
- Spring Security（portal-cms 表單登入，Session 驗證）
- Thymeleaf SSR
- Lombok

---

## 資料庫 Schema

詳細資料表定義請參閱 [docs/database.md](docs/database.md)。

```
site
 └── page
      ├── page_content       ← 多個 Body 區塊，依 sort_order 疊加渲染
      └── page_version       ← 發布時 JSON 快照

layout_set ←── page         ← header_key / footer_key（TemplateKey enum）
component_definition         ← body 元件清單，由 portal-web 啟動時自動掃描同步
asset
cms_user ── cms_user_role
unit
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
  → DeviceInterceptor（Cookie > User-Agent → isMobile）
  → PageController
  → PageRenderService（查詢 status=PUBLISHED）
  → Page + LayoutSet + List<PageContent>
  → Thymeleaf layout/base.html
       th:replace fragments/header/{headerKey}     ← TemplateKey enum
       th:each pageContents                         ← 多個 body block 疊加
         th:replace fragments/body/{block.blockKey} ← component_definition key
       th:replace fragments/footer/{footerKey}     ← TemplateKey enum
```

---

## 開發環境

詳細設定與啟動步驟請參閱 [docs/dev.md](docs/dev.md)。

```bash
docker compose up -d                                                      # 啟動 PostgreSQL（首次自動執行 db/init/*.sql）
./mvnw spring-boot:run -pl portal-web -Dspring-boot.run.profiles=dev     # 前台 → http://localhost:8100（同時同步 component_definition）
./mvnw spring-boot:run -pl portal-cms -Dspring-boot.run.profiles=dev     # 後台 → http://localhost:8200/cms/
```

> **注意**：建議先啟動 portal-web，讓 `ComponentSyncRunner` 完成 body 元件掃描後，CMS 的元件下拉選單才會有資料。

---

## 正式部署

詳細打包與上版步驟請參閱 [docs/prod.md](docs/prod.md)。

```bash
./mvnw package -DskipTests                   # 打包 JAR
# 將 JAR + application-prod.yml 傳輸到伺服器後以 java -jar 執行
```

---

## 模組依賴關係

```
portal-web  ──┐
               ├──→ portal-domain ──→ PostgreSQL
portal-cms  ──┘
```

---

## 新增 Body 元件

AP 只需要做一件事：在 `portal-web/src/main/resources/templates/fragments/body/` 新增 HTML 檔案（如 `rwd_body_04.html`），重新部署 portal-web 後，`ComponentSyncRunner` 會在啟動時自動掃描並將新元件寫入 `component_definition` 表，CMS 下拉選單隨即出現新選項。

無需修改任何 Java 程式碼或 enum。
