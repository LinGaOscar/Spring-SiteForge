CREATE TABLE page (
    id              BIGSERIAL    PRIMARY KEY,
    site_id         BIGINT       NOT NULL REFERENCES site(id),
    path            VARCHAR(255) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    seo_title       VARCHAR(200),
    seo_description VARCHAR(500),
    layout_set_id   BIGINT       REFERENCES layout_set(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    UNIQUE (site_id, path)
);

CREATE TABLE page_content (
    id           BIGSERIAL    PRIMARY KEY,
    page_id      BIGINT       NOT NULL REFERENCES page(id) ON DELETE CASCADE,
    block_key    VARCHAR(100) NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    content_json TEXT         NOT NULL,
    locale       VARCHAR(10)  NOT NULL DEFAULT 'zh-TW',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (page_id, block_key, locale)
);
