ALTER TABLE categories
    ADD COLUMN normalized_name VARCHAR(100);

UPDATE categories
SET normalized_name =
        lower(regexp_replace(btrim(name), '[[:space:]]+', ' ', 'g'));

ALTER TABLE categories
    ALTER COLUMN normalized_name SET NOT NULL;

CREATE TEMP TABLE category_dedup_map ON COMMIT DROP AS
SELECT id AS duplicate_id, keeper_id
FROM (
    SELECT
        id,
        first_value(id) OVER (
            PARTITION BY normalized_name
            ORDER BY
                CASE WHEN status = 'PUBLISHED' THEN 0 ELSE 1 END,
                created_at,
                id
        ) AS keeper_id
    FROM categories
    WHERE status IN ('DRAFT', 'PUBLISHED')
) ranked
WHERE id <> keeper_id;

UPDATE events e
SET category_id = mapping.keeper_id
FROM category_dedup_map mapping
WHERE e.category_id = mapping.duplicate_id;

UPDATE category_requests cr
SET category_id = mapping.keeper_id
FROM category_dedup_map mapping
WHERE cr.category_id = mapping.duplicate_id;

UPDATE categories c
SET status = 'ARCHIVED'
FROM category_dedup_map mapping
WHERE c.id = mapping.duplicate_id;

CREATE UNIQUE INDEX uq_categories_active_normalized_name
    ON categories(normalized_name)
    WHERE status IN ('DRAFT', 'PUBLISHED');

ALTER TABLE category_requests
    ADD COLUMN normalized_proposed_name VARCHAR(100);

UPDATE category_requests
SET normalized_proposed_name =
        lower(regexp_replace(btrim(proposed_name), '[[:space:]]+', ' ', 'g'));

ALTER TABLE category_requests
    ALTER COLUMN normalized_proposed_name SET NOT NULL;

DROP INDEX uq_category_requests_pending_name_per_requester;

CREATE INDEX idx_category_requests_normalized_name_status
    ON category_requests(normalized_proposed_name, status);
