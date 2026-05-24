# 正式部署指南

## 部署方式：Docker Image 打包傳輸（.tar.gz）

適用於內部網路或無對外連線的環境。在開發機建置 Image，打包後以 SCP / FTP / 隨身碟等方式傳到目標伺服器。

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

## 目標伺服器：部署

### 前置需求

- Docker 已安裝
- PostgreSQL 與 Redis 已啟動（可用 `docker compose up -d` 搭配 `docker-compose.yml`）

### 1. 載入 Image

```bash
docker load < /opt/siteforge/portal-cms.tar.gz
docker load < /opt/siteforge/portal-web.tar.gz
```

### 2. 複製必要檔案到伺服器

需要以下檔案（從 git repo 複製或手動建立）：

```
docker-compose.prod.yml
docker/nginx.conf
.env
```

### 3. 確認 .env 內容

```bash
# /opt/siteforge/.env
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

## 更新部署流程

每次有新版本需要更新時：

```bash
# 開發機：重新 build 並打包
docker compose -f docker-compose.prod.yml build
docker save spring-siteforge-portal-cms:latest | gzip > portal-cms.tar.gz
docker save spring-siteforge-portal-web:latest | gzip > portal-web.tar.gz

# 傳輸到伺服器後：
docker load < portal-cms.tar.gz
docker load < portal-web.tar.gz
docker compose -f docker-compose.prod.yml up -d --no-build
```

---

## 注意事項

| 項目 | 說明 |
|------|------|
| Flyway migration | portal-cms 啟動時自動執行，新版本有新 migration 會自動套用 |
| 上傳圖片 | 存放於 Docker volume `uploads`，更新部署不會影響已上傳的檔案 |
| DB 資料 | 存放於 Docker volume `postgres_data`，重新部署不會清除資料 |
| .env 不進 git | 每個環境各自維護自己的 `.env`，不共用 |
