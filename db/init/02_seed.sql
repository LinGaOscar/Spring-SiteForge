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

-- 素材（about 頁使用的示範圖片，Method B：imageUrl 直接指向靜態路徑）
INSERT INTO asset (filename, file_path, mime_type, size, unit_code, uploaded_by, created_by) VALUES
    ('company.png',  '/uploads/about/company.png',  'image/png', 0, '00100', 'system', 'system'),
    ('mission.png',  '/uploads/about/mission.png',  'image/png', 0, '00100', 'system', 'system');

-- Body 區塊
INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale)
SELECT id, 'rwd_body_01', 0, '{}', 'zh-TW' FROM page WHERE path = '/';

-- /about 頁面：4 個元件組合
INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale)
SELECT id, 'rwd_hero', 0,
    '{"badge":"關於我們","title":"深耕數位轉型，陪伴企業成長","subtitle":"自 2015 年起，我們協助超過 500 家企業打造卓越的數位品牌形象，以專業技術與服務贏得客戶信賴。","ctaText":"聯絡我們","ctaUrl":"/contact","ctaText2":"服務項目","ctaUrl2":"/services"}',
    'zh-TW' FROM page WHERE path = '/about';

INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale)
SELECT id, 'rwd_text_image', 1,
    '{"badge":"我們的故事","title":"從一個願景出發","body":"<p>2015 年，我們以「讓每個企業都能擁有專業數位形象」為使命創立。歷經十年深耕，從最初的小型團隊，成長為橫跨多個產業的數位解決方案夥伴。</p><p>我們相信，優質的數位體驗不應只是大企業的專利，因此我們持續開發易於使用的 CMS 平台，讓各規模企業都能高效管理品牌內容。</p>","imageUrl":"http://localhost:8200/cws/uploads/about/company.png","imageAlt":"公司故事照片","imagePosition":"right"}',
    'zh-TW' FROM page WHERE path = '/about';

INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale)
SELECT id, 'rwd_stats', 2,
    '{"stat1":"2015","label1":"創立年份","stat2":"50+","label2":"專業團隊人員","stat3":"500+","label3":"服務企業客戶","stat4":"3","label4":"榮獲產業獎項"}',
    'zh-TW' FROM page WHERE path = '/about';

INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale)
SELECT id, 'rwd_cta', 3,
    '{"title":"準備好開始了嗎？","description":"與我們的團隊聯繫，了解如何為您的企業打造專屬數位解決方案。","btnText":"立即聯絡","btnUrl":"/contact"}',
    'zh-TW' FROM page WHERE path = '/about';

INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale)
SELECT id, 'rwd_body_03', 0, '{}', 'zh-TW' FROM page WHERE path = '/services';
