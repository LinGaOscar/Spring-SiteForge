# 正式環境部署

## 部署方式

在開發機建置 Docker Image，打包成 `.tar.gz` 後傳輸到目標伺服器載入啟動。  
適用於內部網路或無對外連線的環境。

---

## 開發機：建置與打包

### 1. 設定環境變數

```bash
cp .env.example .env
# 編輯 .env，填入正式環境的 DB 密碼
```

### 2. 建置 Docker Image

```bash
docker compose -f docker-compose.prod.yml build
```

### 3. 確認 Image 名稱

```bash
docker images | grep siteforge
```

### 4. 打包成 .tar.gz

```bash
docker save spring-siteforge-portal-cms:latest | gzip > portal-cms.tar.gz
docker save spring-siteforge-portal-web:latest | gzip > portal-web.tar.gz
```

### 5. 傳輸到目標伺服器

```bash
scp portal-cms.tar.gz portal-web.tar.gz user@server:/opt/siteforge/
```

> 也可使用 FTP、隨身碟或公司內部檔案共享，傳輸方式不限。

---

## 目標伺服器：首次部署

### 前置需求

- Docker 已安裝

### 1. 載入 Image

```bash
docker load < /opt/siteforge/portal-cms.tar.gz
docker load < /opt/siteforge/portal-web.tar.gz
```

### 2. 準備設定檔

將以下檔案複製到伺服器工作目錄：

```
docker-compose.prod.yml
docker/nginx.conf
.env
```

### 3. 設定 .env

```bash
POSTGRES_DB=siteforge_db
POSTGRES_USER=siteforge
POSTGRES_PASSWORD=your_real_password
```

### 4. 啟動所有服務

```bash
docker compose -f docker-compose.prod.yml up -d
```

### 5. 確認服務狀態

```bash
docker compose -f docker-compose.prod.yml ps
```

---

## 更新版本

```bash
# 開發機：重新 build 並打包
docker compose -f docker-compose.prod.yml build
docker save spring-siteforge-portal-cms:latest | gzip > portal-cms.tar.gz
docker save spring-siteforge-portal-web:latest | gzip > portal-web.tar.gz

# 傳輸後，目標伺服器執行：
docker load < portal-cms.tar.gz
docker load < portal-web.tar.gz
docker compose -f docker-compose.prod.yml up -d --no-build
```

---

## 注意事項

| 項目 | 說明 |
|------|------|
| Flyway migration | portal-cms 啟動時自動執行，新版本有新 migration 會自動套用 |
| 上傳圖片 | 存於 Docker volume `uploads`，更新部署不影響已上傳的檔案 |
| DB 資料 | 存於 Docker volume `postgres_data`，重新部署不會清除資料 |
| .env | 每個環境各自維護，不進 git |
