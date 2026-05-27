-- ============================================================
-- SpringSiteForge — 種子資料
-- 執行於 01_schema.sql 之後
-- 密碼使用 pgcrypto bcrypt，與 Spring Security BCryptPasswordEncoder 相容
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 固定單位
INSERT INTO unit (code, name) VALUES
    ('00100', '單位 00100'),
    ('00800', '單位 00800'),
    ('00850', '單位 00850');

-- Dev 管理員帳號（OP + MA，屬於 00100）
-- 明文密碼：siteforge2026
INSERT INTO cms_user (username, password, enabled, unit_code)
VALUES ('manager', crypt('siteforge2026', gen_salt('bf', 10)), TRUE, '00100');

INSERT INTO cms_user_role (user_id, role)
SELECT id, 'OP' FROM cms_user WHERE username = 'manager'
UNION ALL
SELECT id, 'MA' FROM cms_user WHERE username = 'manager';

-- manager2：dev 測試用第二帳號（OP + MA，unit 00100）
-- 明文密碼：siteforge2026
INSERT INTO cms_user (username, password, enabled, unit_code)
VALUES ('manager2', crypt('siteforge2026', gen_salt('bf', 10)), TRUE, '00100');

INSERT INTO cms_user_role (user_id, role)
SELECT id, 'OP' FROM cms_user WHERE username = 'manager2'
UNION ALL
SELECT id, 'MA' FROM cms_user WHERE username = 'manager2';

-- 站台
INSERT INTO site (code, name, domain, created_by)
VALUES ('default', 'SpringSiteForge', 'localhost', 'system');

-- 版型（header + footer，body 由 page_content 管理）
INSERT INTO layout_set (name, header_key, footer_key, description, created_by)
VALUES ('企業官網版型', 'RWD_HEADER_01', 'RWD_FOOTER_01', '標準 RWD 企業版型', 'system');

-- 預設頁面（PUBLISHED）
INSERT INTO page (site_id, path, title, seo_title, seo_description, layout_set_id, status, unit_code, created_by, updated_by)
SELECT
    s.id, '/', '首頁',
    'SpringSiteForge — 企業官網解決方案',
    '專業 CMS 平台，協助企業打造卓越數位品牌形象。',
    ls.id, 'PUBLISHED', '00100', 'system', 'system'
FROM site s, layout_set ls WHERE s.code = 'default';

INSERT INTO page (site_id, path, title, seo_title, seo_description, layout_set_id, status, unit_code, created_by, updated_by)
SELECT
    s.id, '/about', '關於我們',
    '關於我們 — SpringSiteForge',
    '深耕企業數位轉型，服務超過 500 家企業客戶。',
    ls.id, 'PUBLISHED', '00100', 'system', 'system'
FROM site s, layout_set ls WHERE s.code = 'default';

INSERT INTO page (site_id, path, title, seo_title, seo_description, layout_set_id, status, unit_code, created_by, updated_by)
SELECT
    s.id, '/services', '服務項目',
    '服務項目 — SpringSiteForge',
    '提供企業官網建置、CMS、數位行銷等全方位解決方案。',
    ls.id, 'PUBLISHED', '00100', 'system', 'system'
FROM site s, layout_set ls WHERE s.code = 'default';

-- Body 區塊
INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale)
SELECT id, 'rwd_body_01', 0, '{}', 'zh-TW' FROM page WHERE path = '/';

INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale)
SELECT id, 'rwd_body_02', 0, '{}', 'zh-TW' FROM page WHERE path = '/about';

INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale)
SELECT id, 'rwd_body_03', 0, '{}', 'zh-TW' FROM page WHERE path = '/services';
