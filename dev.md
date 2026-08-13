# 開發者指南

完整逐步設定請參閱 [docs/dev.md](docs/dev.md)（含帳號、埠號、環境變數對照表）。本檔只列快速指令、測試方式與結構備註。

## 前置需求

- Java 21
- Maven 3.9+（或直接用 `./mvnw`）
- Docker（跑 PostgreSQL + Redis）

## 快速啟動

```bash
cp .env.example .env                                                      # 首次 clone 後
./mvnw install -DskipTests                                                # 安裝 portal-domain 至本機 repo

docker compose up -d                                                      # 啟動 PostgreSQL + Redis（首次自動跑 db/init/*.sql）

./mvnw spring-boot:run -pl portal-web -Dspring-boot.run.profiles=dev      # 前台 → http://localhost:8100/web/
./mvnw spring-boot:run -pl portal-cms -Dspring-boot.run.profiles=dev      # 後台 → http://localhost:8200/cms/
```

建議先啟動 `portal-web`：`ComponentSyncRunner` 會掃描 body/header/footer fragment 並寫入 `component_definition`，CMS 的元件下拉選單才有資料。

重置 DB（清空所有資料重來）：

```bash
docker compose down -v && docker compose up -d
```

Dev 管理員帳號（由 `db/init/02_seed.sql` 建立）：`manager` / `siteforge2026`，單位 `00100`。

## 測試

```bash
./mvnw test -pl portal-web,portal-cms,portal-domain                        # 全套測試
./mvnw test -pl portal-cms -Dtest=LocalStorageServiceTest                  # 單一測試類
```

- `portal-domain` 測試用 H2 in-memory DB（`@DataJpaTest` 系列），不需要 Docker Postgres。
- `portal-web`、`portal-cms` 的 controller/service 測試多為 Mockito 單元測試，同樣不依賴 Docker 或 Redis。
- 因此本地跑測試**不需要先 `docker compose up`**；Docker 只在要「實際啟動應用」時才需要。

## 結構備註

Maven multi-module：

| 模組 | 內容 |
|------|------|
| `portal-domain` | JPA Entity / Repository / enum，純函式庫，不單獨部署 |
| `portal-web` | 前台 Thymeleaf SSR（port 8100，context-path `/web`），無 REST API |
| `portal-cms` | 後台 Thymeleaf SSR + REST API（port 8200，context-path `/cms`），Spring Security Session 表單登入 |

`portal-web` 與 `portal-cms` 都依賴 `portal-domain`，彼此不互相依賴，各自可獨立啟動、共用同一個 PostgreSQL。

其餘設計細節（頁面狀態機、單位隔離、版本快照、元件開發流程等）見專案根目錄 [CLAUDE.md](CLAUDE.md) 與 [README.md](README.md)。
