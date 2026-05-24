# API 文件

## 認證方式

CMS API 使用 Session 認證。呼叫 `/api/cms/auth/login` 取得 Session Cookie 後，後續請求自動帶入。

---

## 前台 Public API（portal-web · port 8100）

無需認證，唯讀，僅返回 `status = PUBLISHED` 的資料。

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/pages/search` | 搜尋頁面 |
| GET | `/api/pages/{path}` | 依路徑取得頁面內容 |
| GET | `/api/assets/{id}` | 取得資產 Metadata |

---

## CMS API（portal-cms · port 8200）

所有 `/api/cms/**` 路徑需先登入。標示 `[MANAGER]` 的端點需要 MANAGER 角色。

### 認證

| 方法 | 路徑 | 說明 |
|------|------|------|
| POST | `/api/cms/auth/login` | 登入，Body: `{ "username": "", "password": "" }` |

---

### 版型管理（LayoutSet）

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/cms/layouts` | 取得所有版型 |
| POST | `/api/cms/layouts` | 建立版型 `[MANAGER]` |
| PUT | `/api/cms/layouts/{id}` | 更新版型 `[MANAGER]` |

---

### 頁面管理（Page）

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/cms/pages` | 取得所有頁面清單 |
| POST | `/api/cms/pages` | 建立頁面 |
| GET | `/api/cms/pages/{id}` | 取得單一頁面 |
| PUT | `/api/cms/pages/{id}` | 更新頁面基本資料 |
| DELETE | `/api/cms/pages/{id}` | 刪除頁面 `[MANAGER]` |

---

### 發布與版本管理

| 方法 | 路徑 | 說明 |
|------|------|------|
| POST | `/api/cms/pages/{id}/publish` | 發布頁面（建立版本快照） |
| POST | `/api/cms/pages/{id}/rollback` | 回滾至指定版本，Body: `{ "versionNo": 1 }` |
| GET | `/api/cms/pages/{id}/versions` | 取得版本歷史清單 |

---

### 頁面內容區塊（PageContent）

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/cms/pages/{pageId}/contents` | 取得頁面所有內容區塊 |
| POST | `/api/cms/pages/{pageId}/contents` | 新增內容區塊 |
| PUT | `/api/cms/pages/{pageId}/contents/{contentId}` | 更新內容區塊 |
| DELETE | `/api/cms/pages/{pageId}/contents/{contentId}` | 刪除內容區塊 |

---

### 資產管理（Asset）

| 方法 | 路徑 | 說明 |
|------|------|------|
| GET | `/api/cms/assets` | 取得所有已上傳資產清單 |
| POST | `/api/cms/assets` | 手動建立資產 Metadata（不含上傳） |
| POST | `/api/cms/assets/upload` | 上傳圖片（multipart/form-data，欄位名 `file`） |

#### 圖片上傳限制

- 最大檔案大小：**10 MB**
- 支援格式：`image/jpeg`、`image/png`、`image/gif`、`image/webp`
- 儲存路徑：`./uploads/{year}/{mm}/{uuid}.{ext}`
- 存取 URL：`http://localhost:8200/uploads/{year}/{mm}/{uuid}.{ext}`

---

## 統一回應格式

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
    "message": "filename is required"
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
