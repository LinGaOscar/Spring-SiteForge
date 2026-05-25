# API 文件

## 架構說明

本專案包含兩個應用：

| 應用 | Port | 性質 |
|------|------|------|
| `portal-web` | 8100 | 純 SSR（Thymeleaf），無 REST API |
| `portal-cms` | 8200 | Thymeleaf UI + REST API 並存 |

`portal-web` 為 Server-Side Rendering，不對外暴露 JSON API。

---

## CMS REST API（portal-cms · port 8200）

所有 `/api/cms/**` 路徑需先透過表單登入（`/cms/auth/login`），後續請求自動帶入 Session Cookie。

---

### 版型管理（LayoutSet）

> 版型僅儲存 `headerKey` 與 `footerKey`，body 區塊改由頁面內容區塊（PageContent）管理。

| 方法 | 路徑 | 角色 | 說明 |
|------|------|------|------|
| GET | `/api/cms/layouts` | OP / MA | 取得所有版型 |
| POST | `/api/cms/layouts` | MA | 建立版型，Body: `{ "name": "", "headerKey": "", "footerKey": "" }` |
| PUT | `/api/cms/layouts/{id}` | MA | 更新版型 |

---

### 頁面管理（Page）

| 方法 | 路徑 | 角色 | 說明 |
|------|------|------|------|
| GET | `/api/cms/pages` | OP / MA | 取得頁面清單（本單位） |
| POST | `/api/cms/pages` | OP | 建立頁面 |
| GET | `/api/cms/pages/{id}` | OP / MA | 取得單一頁面 |
| PUT | `/api/cms/pages/{id}` | OP | 更新頁面基本資料 |
| DELETE | `/api/cms/pages/{id}` | MA | 刪除頁面 |

---

### 頁面內容區塊（PageContent）

`blockKey` 必須是 `TemplateKey` enum 中的 Body 類型（大小寫不拘），例如 `rwd_body_01`、`rwd_body`。

| 方法 | 路徑 | 角色 | 說明 |
|------|------|------|------|
| GET | `/api/cms/pages/{pageId}/contents` | OP / MA | 取得頁面所有 Body 區塊（依 sortOrder 排序） |
| POST | `/api/cms/pages/{pageId}/contents` | OP | 新增 Body 區塊 |
| PUT | `/api/cms/pages/{pageId}/contents/{contentId}` | OP | 更新 Body 區塊 |
| DELETE | `/api/cms/pages/{pageId}/contents/{contentId}` | OP | 刪除 Body 區塊 |

**POST / PUT Body：**
```json
{
  "blockKey": "rwd_body_01",
  "sortOrder": 0,
  "contentJson": "{}",
  "locale": "zh-TW"
}
```

---

### 發布與版本管理

> 以下端點目前使用 `hasRole('MANAGER')` 授權，對應 MA 角色。

| 方法 | 路徑 | 角色 | 說明 |
|------|------|------|------|
| POST | `/api/cms/pages/{id}/publish` | MA | 放行發布（建立 `page_version` 快照） |
| POST | `/api/cms/pages/{id}/rollback` | MA | 回滾至指定版本 |
| GET | `/api/cms/pages/{id}/versions` | OP / MA | 查看版本歷史清單 |

**Rollback Body：**
```json
{ "versionId": 5 }
```

回滾後：頁面狀態自動設為 `PUBLISHED`，並建立新版本快照。

---

### 資產管理（Asset）

| 方法 | 路徑 | 角色 | 說明 |
|------|------|------|------|
| GET | `/api/cms/assets` | 全員 | 取得資產清單（可跨單位瀏覽） |
| POST | `/api/cms/assets` | OP / MA | 建立資產 Metadata（不含上傳） |
| POST | `/api/cms/assets/upload` | OP / MA | 上傳檔案（`multipart/form-data`，欄位名 `file`） |

**上傳限制：**

- 最大檔案大小：**10 MB**
- 支援格式：`jpg`、`jpeg`、`png`、`gif`、`webp`、`pdf`、`doc`、`docx`、`xls`、`xlsx`
- 不支援 SVG（防止 Stored XSS）
- 儲存路徑：`/uploads/{year}/{mm}/{uuid}.{ext}`

---

## CMS 後台 UI 路由（Thymeleaf，非 REST）

以下路由為 HTML 頁面，供瀏覽器操作使用，不回傳 JSON。

```
GET  /cms/auth/login                   登入頁
GET  /cms/dashboard                    統計 + MA 待辦清單
GET  /cms/pages                        頁面列表
GET  /cms/pages/new                    新增頁面（OP）
GET  /cms/pages/{id}/edit              編輯頁面（OP，DRAFT / APPROVED）
POST /cms/pages/{id}/submit            OP 送審
POST /cms/pages/{id}/approve           MA 放行建立
POST /cms/pages/{id}/reject            MA 退回建立
POST /cms/pages/{id}/request-publish   OP 申請發布
POST /cms/pages/{id}/approve-publish   MA 放行發布
POST /cms/pages/{id}/reject-publish    MA 退回發布
POST /cms/pages/{id}/request-unpublish OP 申請下架
POST /cms/pages/{id}/confirm-unpublish MA 確認下架
POST /cms/pages/{id}/unpublish         MA 直接下架
GET  /cms/assets                       素材管理
POST /cms/assets/upload                上傳素材（UI 表單）
POST /cms/assets/{id}/delete           刪除素材（同單位）
```

---

## 統一 REST 回應格式

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

錯誤時：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "BAD_REQUEST",
    "message": "blockKey 必須是合法的 Body TemplateKey"
  }
}
```

| HTTP 狀態碼 | 情境 |
|------------|------|
| 200 | 成功 |
| 201 | 建立成功 |
| 400 | 請求參數錯誤 |
| 401 | 未登入 |
| 403 | 無權限 |
| 409 | 資料衝突（唯一鍵重複） |
| 500 | 伺服器錯誤 |
