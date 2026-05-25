-- V7: layout_set.body_key 改為 nullable，body 區塊改由 page_content 管理

-- 先為所有透過 layout_set.body_key 配置 body 的頁面建立 page_content（保留現有 body）
INSERT INTO page_content (page_id, block_key, sort_order, content_json, locale, created_at, updated_at)
SELECT p.id, LOWER(ls.body_key), 0, '{}', 'zh-TW', NOW(), NOW()
FROM page p
JOIN layout_set ls ON p.layout_set_id = ls.id
WHERE ls.body_key IS NOT NULL
ON CONFLICT (page_id, block_key, locale) DO NOTHING;

ALTER TABLE layout_set ALTER COLUMN body_key DROP NOT NULL;
UPDATE layout_set SET body_key = NULL;
