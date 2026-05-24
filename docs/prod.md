# 正式環境部署

## 部署方式

在開發機建置 Docker Image，打包成 `.tar.gz` 後傳輸到目標伺服器載入啟動。  
適用於內部網路或無對外連線的環境。

---

## 開發機：建置與打包

### 1. 建置 Docker Image

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

傳輸 Image 與設定檔，**不傳 `.env`**（密碼須在伺服器上直接建立）：

```bash
scp portal-cms.tar.gz portal-web.tar.gz user@server:/opt/siteforge/
scp docker-compose.prod.yml user@server:/opt/siteforge/
scp docker/nginx.conf user@server:/opt/siteforge/docker/
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

### 2. 在伺服器上直接建立 .env

> 公司規定密碼只能在正式環境填入，`.env` 不經過任何傳輸媒介。

```bash
ssh user@server
cd /opt/siteforge

cat > .env << 'EOF'
POSTGRES_DB=siteforge_db
POSTGRES_USER=siteforge
POSTGRES_PASSWORD=正式密碼填這裡
EOF
```

### 3. 啟動所有服務

```bash
docker compose -f docker-compose.prod.yml up -d
```

### 4. 確認服務狀態

```bash
docker compose -f docker-compose.prod.yml ps
```

---

## 更新版本

`.env` 已在伺服器上，更新時不需重新建立，只傳 Image 即可：

```bash
# 開發機：重新 build 並打包
docker compose -f docker-compose.prod.yml build
docker save spring-siteforge-portal-cms:latest | gzip > portal-cms.tar.gz
docker save spring-siteforge-portal-web:latest | gzip > portal-web.tar.gz

# 傳輸（不傳 .env）
scp portal-cms.tar.gz portal-web.tar.gz user@server:/opt/siteforge/

# 目標伺服器執行
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
