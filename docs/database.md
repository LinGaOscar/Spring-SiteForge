# 資料庫文件

## 概覽

資料庫：PostgreSQL 16  
Schema 管理：Flyway（portal-cms 啟動時自動執行）  
Migration 腳本位置：`portal-cms/src/main/resources/db/migration/`

### 資料表關聯

```
site
 └── page  (site_id FK)
      ├── page_content  (page_id FK, CASCADE DELETE)
      └── page_version  (page_id FK, CASCADE DELETE)

layout_set ←── page (layout_set_id FK)

asset  (獨立資料表)

cms_user ──── cms_user_role ──── cms_role
```

---

## 資料表定義

### `site` — 站台

| 欄位 | 型別 | 限制 | 說明 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| code | VARCHAR(50) | NOT NULL UNIQUE | 站台代碼 |
| name | VARCHAR(100) | NOT NULL | 站台名稱 |
| domain | VARCHAR(200) | | 網域 |
| default_locale | VARCHAR(10) | NOT NULL DEFAULT `zh-TW` | 預設語系 |
| enabled | BOOLEAN | NOT NULL DEFAULT TRUE | |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| created_by | VARCHAR(100) | | |
| updated_by | VARCHAR(100) | | |

---

### `layout_set` — 版型組合

| 欄位 | 型別 | 限制 | 說明 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| name | VARCHAR(100) | NOT NULL | 版型名稱 |
| header_key | VARCHAR(50) | NOT NULL | Header 模板 key（來自 TemplateKey enum） |
| body_key | VARCHAR(50) | NOT NULL | Body 模板 key |
| footer_key | VARCHAR(50) | NOT NULL | Footer 模板 key |
| description | VARCHAR(500) | | |
| enabled | BOOLEAN | NOT NULL DEFAULT TRUE | |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| created_by | VARCHAR(100) | | |
| updated_by | VARCHAR(100) | | |

---

### `page` — 頁面

| 欄位 | 型別 | 限制 | 說明 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| site_id | BIGINT | NOT NULL FK → site | |
| path | VARCHAR(255) | NOT NULL | URL 路徑，如 `/about` |
| title | VARCHAR(200) | NOT NULL | 頁面標題 |
| seo_title | VARCHAR(200) | | SEO 標題 |
| seo_description | VARCHAR(500) | | SEO 描述 |
| layout_set_id | BIGINT | FK → layout_set | 可為 NULL |
| status | VARCHAR(20) | NOT NULL DEFAULT `DRAFT` | `DRAFT` / `PUBLISHED` |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| created_by | VARCHAR(100) | | |
| updated_by | VARCHAR(100) | | |

**唯一約束**：`(site_id, path)`

---

### `page_content` — 頁面內容區塊

| 欄位 | 型別 | 限制 | 說明 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| page_id | BIGINT | NOT NULL FK → page CASCADE | |
| block_key | VARCHAR(100) | NOT NULL | 區塊識別 key |
| sort_order | INT | NOT NULL DEFAULT 0 | 排列順序 |
| content_json | TEXT | NOT NULL | 內容 JSON |
| locale | VARCHAR(10) | NOT NULL DEFAULT `zh-TW` | 語系 |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

**唯一約束**：`(page_id, block_key, locale)`

---

### `page_version` — 頁面版本快照

| 欄位 | 型別 | 限制 | 說明 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| page_id | BIGINT | NOT NULL FK → page CASCADE | |
| version_no | INT | NOT NULL | 版本號（從 1 累加） |
| snapshot_json | TEXT | NOT NULL | 發布當下完整頁面狀態 JSON |
| status | VARCHAR(20) | NOT NULL | `DRAFT` / `PUBLISHED` |
| published_at | TIMESTAMP | | 發布時間 |
| published_by | VARCHAR(100) | | 發布人 |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

**唯一約束**：`(page_id, version_no)`

> 回滾操作會建立新版本（不覆蓋舊記錄），並從指定版本的 `snapshot_json` 還原頁面狀態。

---

### `asset` — 上傳資產

| 欄位 | 型別 | 限制 | 說明 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| filename | VARCHAR(255) | NOT NULL | 原始檔名（經過安全消毒） |
| file_path | VARCHAR(500) | NOT NULL | 存取路徑，如 `/uploads/2026/05/{uuid}.png` |
| mime_type | VARCHAR(100) | | MIME 類型 |
| size | BIGINT | | 檔案大小（bytes） |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| created_by | VARCHAR(100) | | 上傳者帳號 |

---

### `cms_user` — CMS 使用者

| 欄位 | 型別 | 限制 | 說明 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| username | VARCHAR(50) | NOT NULL UNIQUE | |
| password | VARCHAR(255) | NOT NULL | BCrypt 加密 |
| enabled | BOOLEAN | NOT NULL DEFAULT TRUE | |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL DEFAULT NOW() | |

### `cms_role` — CMS 角色

| 欄位 | 型別 | 限制 | 說明 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| name | VARCHAR(50) | NOT NULL UNIQUE | 如 `ROLE_EDITOR`、`ROLE_MANAGER` |

### `cms_user_role` — 使用者角色關聯（多對多）

| 欄位 | 型別 | 限制 |
|------|------|------|
| user_id | BIGINT | FK → cms_user CASCADE |
| role_id | BIGINT | FK → cms_role CASCADE |

---

## Flyway Migration 歷史

| 版本 | 檔案 | 建立的資料表 |
|------|------|-------------|
| V1 | `V1__create_cms_user_tables.sql` | `cms_role`、`cms_user`、`cms_user_role` |
| V2 | `V2__create_site_layout_tables.sql` | `site`、`layout_set` |
| V3 | `V3__create_page_tables.sql` | `page`、`page_content` |
| V4 | `V4__create_version_asset_tables.sql` | `page_version`、`asset` |
