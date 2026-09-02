-- =====================================================================
-- TIKZY - V6: Login failure tracking and admin-configurable lock policy
-- =====================================================================

ALTER TABLE users
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN locked_at TIMESTAMP;

ALTER TABLE users
    ADD CONSTRAINT chk_users_failed_login_attempts
        CHECK (failed_login_attempts >= 0);

CREATE TABLE security_policies (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    max_failed_login_attempts  INTEGER NOT NULL DEFAULT 5,
    created_at                 TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_security_policies_max_failed_login_attempts
        CHECK (max_failed_login_attempts > 0)
);

INSERT INTO security_policies (max_failed_login_attempts)
VALUES (5);
