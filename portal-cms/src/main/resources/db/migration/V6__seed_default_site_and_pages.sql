-- 企業形象網站預設種資料（冪等，可重複執行）

-- 站台
INSERT INTO site (code, name, domain, default_locale, enabled, created_by)
VALUES ('default', 'SpringSiteForge', 'localhost', 'zh-TW', true, 'system')
ON CONFLICT (code) DO NOTHING;

-- Layout Sets（三組版型）
INSERT INTO layout_set (name, header_key, body_key, footer_key, description, enabled, created_by)
VALUES
    ('首頁版型',   'RWD_HEADER_01', 'RWD_BODY_01', 'RWD_FOOTER_01', '企業官網首頁', true, 'system'),
    ('關於我們版型', 'RWD_HEADER_01', 'RWD_BODY_02', 'RWD_FOOTER_01', '公司簡介頁面', true, 'system'),
    ('服務項目版型', 'RWD_HEADER_01', 'RWD_BODY_03', 'RWD_FOOTER_01', '服務介紹頁面', true, 'system')
ON CONFLICT DO NOTHING;

-- 三個預設頁面（PUBLISHED，使用 unit_code = 00100）
-- 首頁 /
INSERT INTO page (site_id, path, title, seo_title, seo_description, layout_set_id, status, unit_code, created_by, updated_by)
SELECT
    s.id,
    '/',
    '首頁',
    'SpringSiteForge — 企業官網解決方案',
    '專業 CMS 平台，協助企業打造卓越數位品牌形象，提供響應式設計、內容管理與技術支援。',
    ls.id,
    'PUBLISHED',
    '00100',
    'system',
    'system'
FROM site s
CROSS JOIN layout_set ls
WHERE s.code = 'default' AND ls.name = '首頁版型'
ON CONFLICT (site_id, path) DO NOTHING;

-- 關於我們 /about
INSERT INTO page (site_id, path, title, seo_title, seo_description, layout_set_id, status, unit_code, created_by, updated_by)
SELECT
    s.id,
    '/about',
    '關於我們',
    '關於我們 — SpringSiteForge',
    '深耕企業數位轉型超過十年，服務超過 500 家企業客戶，成就每個品牌的數位未來。',
    ls.id,
    'PUBLISHED',
    '00100',
    'system',
    'system'
FROM site s
CROSS JOIN layout_set ls
WHERE s.code = 'default' AND ls.name = '關於我們版型'
ON CONFLICT (site_id, path) DO NOTHING;

-- 服務項目 /services
INSERT INTO page (site_id, path, title, seo_title, seo_description, layout_set_id, status, unit_code, created_by, updated_by)
SELECT
    s.id,
    '/services',
    '服務項目',
    '服務項目 — SpringSiteForge',
    '提供企業官網建置、CMS 系統、數位行銷整合、雲端主機管理等全方位解決方案。',
    ls.id,
    'PUBLISHED',
    '00100',
    'system',
    'system'
FROM site s
CROSS JOIN layout_set ls
WHERE s.code = 'default' AND ls.name = '服務項目版型'
ON CONFLICT (site_id, path) DO NOTHING;
