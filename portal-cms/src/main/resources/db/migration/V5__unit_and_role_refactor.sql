-- 重構角色系統：以字串直接儲存角色，移除舊 cms_role table
DROP TABLE IF EXISTS cms_user_role;
DROP TABLE IF EXISTS cms_role;

-- 新角色 junction table（OP / MA / VI 直接存字串）
CREATE TABLE cms_user_role (
    user_id BIGINT      NOT NULL REFERENCES cms_user(id) ON DELETE CASCADE,
    role    VARCHAR(10) NOT NULL CHECK (role IN ('OP','MA','VI')),
    PRIMARY KEY (user_id, role)
);

-- 固定單位表
CREATE TABLE unit (
    code       VARCHAR(5)   NOT NULL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- cms_user 加入所屬單位
ALTER TABLE cms_user ADD COLUMN unit_code VARCHAR(5) REFERENCES unit(code);

-- page 加入單位與審核欄位
ALTER TABLE page ADD COLUMN unit_code    VARCHAR(5)   REFERENCES unit(code);
ALTER TABLE page ADD COLUMN review_note  VARCHAR(500);
ALTER TABLE page ADD COLUMN submitted_at TIMESTAMP;
ALTER TABLE page ADD COLUMN submitted_by VARCHAR(100);

-- asset 加入單位與上傳人
ALTER TABLE asset ADD COLUMN unit_code   VARCHAR(5)   REFERENCES unit(code);
ALTER TABLE asset ADD COLUMN uploaded_by VARCHAR(100);

-- 更新 page.status 預設值（對齊新狀態機）
ALTER TABLE page ALTER COLUMN status SET DEFAULT 'DRAFT';

-- 種子資料：三個固定單位
INSERT INTO unit (code, name) VALUES
    ('00100', '單位 00100'),
    ('00800', '單位 00800'),
    ('00850', '單位 00850');
