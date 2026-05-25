# CMS 操作手冊

本文件說明 portal-cms（port 8200）的操作流程，適用於 OP（操作員）與 MA（主管）角色。

---

## 角色說明

| 角色 | 代號 | 權限摘要 |
|------|------|----------|
| 操作員 | OP | 建立/編輯頁面、上傳素材、送審、申請發布/下架 |
| 主管 | MA | 審核、放行發布/下架、直接下架、管理所有操作 |
| 檢視者 | VI | 唯讀，僅能瀏覽 |

- 一個帳號可同時持有多個角色（例如 dev 帳號同時持有 OP + MA）。
- 所有操作限於**同單位**的頁面與素材；跨單位資料僅可瀏覽素材。
- MA **不能**審核自己送出的申請（自審防護）。

---

## 頁面生命週期

```
DRAFT
  │  OP：送審
  ▼
PENDING_REVIEW
  │  MA：放行      │  MA：退回 → 回到 DRAFT
  ▼
APPROVED
  │  OP：申請發布
  ▼
PENDING_PUBLISH
  │  MA：放行發布  │  MA：退回 → 回到 APPROVED
  ▼
PUBLISHED
  │  OP：申請下架                   MA：直接下架
  ▼                                     ▼
PENDING_UNPUBLISH                    APPROVED
  │  MA：確認下架
  ▼
APPROVED
```

---

## 建立新頁面（完整流程）

### 步驟 1：填寫基本資訊

1. 進入 **頁面管理 → 新增頁面**（需 OP 角色）
2. 填寫：
   - **路徑**：以 `/` 開頭，例如 `/about` 或 `/products/intro`
   - **標題**：頁面名稱
   - **SEO 標題**（選填）：瀏覽器 `<title>` 標籤
   - **SEO 描述**（選填）：`<meta name="description">`
3. 選擇**站台**（新增時必填，只需設定一次）

### 步驟 2：設定版面（Header / Footer）

在同一表單的「版面設定」區塊：

- **Header 下拉**：選擇頁首樣式（`RWD_HEADER_01` 為企業官網標準導覽列）
- **Footer 下拉**：選擇頁尾樣式（`RWD_FOOTER_01` 為企業官網標準頁尾）

> RWD 模板支援桌面與手機（Bootstrap responsive）。
> RWS 模板為手機限定，桌面連入會顯示「請使用手機開啟」提示頁。

點擊「**建立（草稿）**」儲存，系統自動進入編輯頁。

### 步驟 3：加入 Body 區塊

建立後自動進入編輯頁，右欄為「**Body 區塊**」管理器：

1. 從下拉選單選擇要加入的 Body 模板
2. 點擊「**+ 加入**」
3. 可重複加入多個區塊，依加入順序由上至下顯示
4. 點「✕」可移除單一區塊

**可用 Body 模板說明：**

| 模板 | 用途 |
|------|------|
| `RWD_BODY_01` | 企業首頁（英雄區 + 統計數字 + 服務特色 + CTA） |
| `RWD_BODY_02` | 關於我們（公司介紹 + 使命願景 + CTA） |
| `RWD_BODY_03` | 服務項目（服務卡片列表 + 流程說明 + CTA） |
| `RWD_BODY` | 通用內容（由 page_content 資料驅動，支援 hero / features / stats / cta 區塊） |
| `RWS_BODY` | 手機限定活動頁（與 RWS_HEADER / RWS_FOOTER 搭配使用） |

> 頁面可疊加多個 Body 模板，例如首頁 = `RWD_BODY_01` + `RWD_BODY_03`。

### 步驟 4：送審與發布

| 動作 | 執行角色 | 說明 |
|------|----------|------|
| 送審 | OP | DRAFT → PENDING_REVIEW |
| 放行建立 | MA（非送審人） | PENDING_REVIEW → APPROVED |
| 退回建立 | MA | PENDING_REVIEW → DRAFT，可附退回原因 |
| 申請發布 | OP | APPROVED → PENDING_PUBLISH |
| 放行發布 | MA（非送審人） | PENDING_PUBLISH → PUBLISHED，建立版本快照 |
| 退回發布 | MA | PENDING_PUBLISH → APPROVED |

---

## 編輯已發布頁面

已發布（PUBLISHED）頁面不可直接編輯。流程：

1. MA 直接下架（PUBLISHED → APPROVED）
2. OP 編輯（APPROVED 狀態可編輯）
3. 重新走「申請發布 → 放行發布」流程

或由 OP 申請下架 → MA 確認下架 → OP 編輯 → 重新發布。

---

## 素材管理

進入 **素材管理**，所有單位的素材均可瀏覽。

**上傳（OP / MA）：**
- 點擊「選擇檔案」後上傳
- 支援格式：`jpg`、`jpeg`、`png`、`gif`、`webp`、`pdf`、`doc`、`docx`、`xls`、`xlsx`
- 不支援 SVG（防止 stored XSS）
- 上傳後的存取路徑：`/uploads/{year}/{mm}/{uuid}.{ext}`

**刪除（同單位 OP / MA）：**
- 只能刪除本單位上傳的素材
- 點擊素材卡片右下角的「刪除」按鈕

---

## 版本歷史與回滾

每次 MA 放行發布時，系統自動建立 `page_version` 快照（儲存路徑、標題、SEO、Header/Footer 設定、所有 Body 區塊）。

**查看版本與回滾**（透過 REST API）：

```
GET  /api/cms/pages/{id}/versions      # 查看版本列表
POST /api/cms/pages/{id}/rollback      # Body: { "versionId": 5 }
```

回滾後：
- 頁面狀態自動設為 PUBLISHED
- 工作流欄位（submittedBy / reviewNote）自動清除
- 建立新版本快照（記錄此次回滾）

---

## Dashboard 說明

登入後首先看到 Dashboard：

- **統計卡片**：各狀態頁面數量
- **待處理清單**（MA 專屬）：列出 `PENDING_REVIEW`、`PENDING_PUBLISH`、`PENDING_UNPUBLISH` 中、且非自己送出的申請，可直接點擊頁面名稱前往操作
