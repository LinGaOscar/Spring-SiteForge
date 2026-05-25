-- V7: layout_set.body_key 改為 nullable，body 區塊改由 page_content 管理

ALTER TABLE layout_set ALTER COLUMN body_key DROP NOT NULL;
UPDATE layout_set SET body_key = NULL;

-- 補齊 V6 種資料的 body 區塊（page_content）
INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale, created_at, updated_at)
SELECT p.id, 'rwd_body_01', 0, '{}', 'zh-TW', NOW(), NOW()
FROM page p JOIN site s ON p.site_id = s.id
WHERE s.code = 'default' AND p.path = '/'
ON CONFLICT (page_id, block_key, locale) DO NOTHING;

INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale, created_at, updated_at)
SELECT p.id, 'rwd_body_02', 0, '{}', 'zh-TW', NOW(), NOW()
FROM page p JOIN site s ON p.site_id = s.id
WHERE s.code = 'default' AND p.path = '/about'
ON CONFLICT (page_id, block_key, locale) DO NOTHING;

INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale, created_at, updated_at)
SELECT p.id, 'rwd_body_03', 0, '{}', 'zh-TW', NOW(), NOW()
FROM page p JOIN site s ON p.site_id = s.id
WHERE s.code = 'default' AND p.path = '/services'
ON CONFLICT (page_id, block_key, locale) DO NOTHING;
