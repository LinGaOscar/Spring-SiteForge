# 開發環境設定

## 前置需求

- Java 21
- Maven 3.9+
- Docker（用於啟動 PostgreSQL + Redis）

---

## 0. 初始化（首次 clone 後執行一次）

在專案根目錄執行，確保所有模組依賴正確安裝至本機 Maven repository：

```bash
# macOS / Linux
./mvnw install -DskipTests

# Windows（Command Prompt 或 PowerShell）
mvnw.cmd install -DskipTests
```

> 專案內建 Maven Wrapper（`mvnw` / `mvnw.cmd`），不需要另外安裝 Maven。
>
> **Windows 注意**：CMD 請使用 `mvnw.cmd`；PowerShell 可用 `.\mvnw` 或 `.\mvnw.cmd`。

---

## 1. 啟動基礎設施

```bash
# macOS / Linux：複製環境變數範本並填入密碼
cp .env.example .env

# Windows CMD
copy .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

```bash
# 啟動 PostgreSQL + Redis（三平台相同）
docker compose up -d
```

預設連線資訊（`.env.example` 預設值）：

| 項目 | 值 |
|------|-----|
| PostgreSQL host | `localhost:5432` |
| Database | `siteforge_db` |
| Username | `siteforge` |
| Redis host | `localhost:6379` |

---

## 2. 啟動 portal-cms（後台 + Flyway migration）

```bash
# macOS / Linux
./mvnw spring-boot:run -pl portal-cms -Dspring-boot.run.profiles=dev

# Windows
mvnw.cmd spring-boot:run -pl portal-cms -Dspring-boot.run.profiles=dev
```

首次啟動會自動執行 Flyway migration 建立所有資料表，並透過 `DataInitializer` 建立預設管理員帳號。

預設管理員帳號設定於 `portal-cms/src/main/resources/application-dev.yml`：

```yaml
cms:
  init:
    admin-username: manager
    admin-password: siteforge2026
```

---

## 3. 啟動 portal-web（前台）

```bash
# macOS / Linux
./mvnw spring-boot:run -pl portal-web -Dspring-boot.run.profiles=dev

# Windows
mvnw.cmd spring-boot:run -pl portal-web -Dspring-boot.run.profiles=dev
```

兩個應用可獨立啟動，`portal-web` 不依賴 `portal-cms` 服務。

---

## 存取位址

| 應用 | URL |
|------|-----|
| 前台 | http://localhost:8100 |
| 後台 CMS | http://localhost:8200 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

> **圖片存取**：Dev 環境的圖片 URL 前綴為 `http://localhost:8200`（已設定於 `cms.asset-base-url`），因為上傳圖片統一由 portal-cms 的 `/uploads/**` 提供。

---

## 執行測試

```bash
# macOS / Linux
./mvnw test
./mvnw test -pl portal-cms
./mvnw test -pl portal-cms -Dtest=LocalStorageServiceTest  # 單一測試類

# Windows CMD
mvnw.cmd test
mvnw.cmd test -pl portal-cms
mvnw.cmd test -pl portal-cms -Dtest=LocalStorageServiceTest

# Windows PowerShell（-D 參數含等號時需加引號）
.\mvnw.cmd test -pl portal-cms "-Dtest=LocalStorageServiceTest"
```
