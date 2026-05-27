-- ============================================================
-- SpringSiteForge — 完整 Schema（最終版，含所有欄位）
-- 由 Docker Compose 在首次建立容器時執行
-- ============================================================

-- 固定單位表
CREATE TABLE unit (
    code       VARCHAR(5)   NOT NULL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- CMS 使用者
CREATE TABLE cms_user (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    unit_code  VARCHAR(5)   REFERENCES unit(code),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 角色（OP / MA / VI 直接存字串）
CREATE TABLE cms_user_role (
    user_id BIGINT      NOT NULL REFERENCES cms_user(id) ON DELETE CASCADE,
    role    VARCHAR(10) NOT NULL CHECK (role IN ('OP','MA','VI')),
    PRIMARY KEY (user_id, role)
);

-- 站台
CREATE TABLE site (
    id             BIGSERIAL    PRIMARY KEY,
    code           VARCHAR(50)  NOT NULL UNIQUE,
    name           VARCHAR(100) NOT NULL,
    domain         VARCHAR(200),
    default_locale VARCHAR(10)  NOT NULL DEFAULT 'zh-TW',
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100)
);

-- 版型（body_key nullable：body 區塊改由 page_content 管理）
CREATE TABLE layout_set (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    header_key  VARCHAR(50)  NOT NULL,
    body_key    VARCHAR(50),
    footer_key  VARCHAR(50)  NOT NULL,
    description VARCHAR(500),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

-- 頁面
CREATE TABLE page (
    id              BIGSERIAL    PRIMARY KEY,
    site_id         BIGINT       NOT NULL REFERENCES site(id),
    path            VARCHAR(255) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    seo_title       VARCHAR(200),
    seo_description VARCHAR(500),
    layout_set_id   BIGINT       REFERENCES layout_set(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    unit_code       VARCHAR(5)   REFERENCES unit(code),
    header_config_json TEXT,
    footer_config_json TEXT,
    review_note     VARCHAR(500),
    submitted_at    TIMESTAMP,
    submitted_by    VARCHAR(100),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    UNIQUE (site_id, path)
);

-- Body 區塊（多區塊疊加，依 sort_order 排序渲染；同一頁可重複使用相同 block_key）
CREATE TABLE page_content (
    id           BIGSERIAL    PRIMARY KEY,
    page_id      BIGINT       NOT NULL REFERENCES page(id) ON DELETE CASCADE,
    block_key    VARCHAR(100) NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    content_json TEXT         NOT NULL,
    locale       VARCHAR(10)  NOT NULL DEFAULT 'zh-TW',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 版本快照（每次發布時建立）
CREATE TABLE page_version (
    id            BIGSERIAL    PRIMARY KEY,
    page_id       BIGINT       NOT NULL REFERENCES page(id) ON DELETE CASCADE,
    version_no    INT          NOT NULL,
    snapshot_json TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    published_at  TIMESTAMP,
    published_by  VARCHAR(100),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (page_id, version_no)
);

-- 素材
CREATE TABLE asset (
    id          BIGSERIAL    PRIMARY KEY,
    filename    VARCHAR(255) NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    mime_type   VARCHAR(100),
    size        BIGINT,
    unit_code   VARCHAR(5)   REFERENCES unit(code),
    uploaded_by VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100)
);

-- 元件定義（由 portal-web 啟動時掃描 fragment 自動同步）
CREATE TABLE component_definition (
    key         VARCHAR(100) NOT NULL PRIMARY KEY,
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('BODY','HEADER','FOOTER')),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    synced_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    schema_json TEXT,
    device_mode VARCHAR(20)  NOT NULL DEFAULT 'RWD'
);
