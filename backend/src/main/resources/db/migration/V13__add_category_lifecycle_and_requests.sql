ALTER TABLE categories
    ADD COLUMN status VARCHAR(20);

UPDATE categories
SET status = CASE
    WHEN is_active THEN 'PUBLISHED'
    ELSE 'ARCHIVED'
END;

ALTER TABLE categories
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'DRAFT',
    ADD CONSTRAINT chk_categories_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    DROP COLUMN is_active;

CREATE TABLE category_requests (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id  UUID         NOT NULL REFERENCES users(id),
    proposed_name VARCHAR(100) NOT NULL,
    description   VARCHAR(1000),
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    review_note   VARCHAR(1000),
    reviewed_by   UUID REFERENCES users(id),
    reviewed_at   TIMESTAMP,
    category_id   UUID REFERENCES categories(id),
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_category_requests_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_category_requests_review
        CHECK (
            (status = 'PENDING'
                AND reviewed_by IS NULL
                AND reviewed_at IS NULL
                AND category_id IS NULL)
            OR (status = 'APPROVED'
                AND reviewed_by IS NOT NULL
                AND reviewed_at IS NOT NULL
                AND category_id IS NOT NULL)
            OR (status = 'REJECTED'
                AND reviewed_by IS NOT NULL
                AND reviewed_at IS NOT NULL
                AND category_id IS NULL
                AND review_note IS NOT NULL)
        )
);

CREATE INDEX idx_category_requests_status_created_at
    ON category_requests(status, created_at);

CREATE INDEX idx_category_requests_requester_id
    ON category_requests(requester_id);

CREATE UNIQUE INDEX uq_category_requests_pending_name_per_requester
    ON category_requests(requester_id, lower(proposed_name))
    WHERE status = 'PENDING';
