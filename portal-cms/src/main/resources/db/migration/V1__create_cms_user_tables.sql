CREATE TABLE cms_role (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE cms_user (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE cms_user_role (
    user_id BIGINT NOT NULL REFERENCES cms_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES cms_role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);
