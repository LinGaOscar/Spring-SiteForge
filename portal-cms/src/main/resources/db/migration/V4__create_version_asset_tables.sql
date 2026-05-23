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

CREATE TABLE asset (
    id         BIGSERIAL    PRIMARY KEY,
    filename   VARCHAR(255) NOT NULL,
    file_path  VARCHAR(500) NOT NULL,
    mime_type  VARCHAR(100),
    size       BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100)
);
