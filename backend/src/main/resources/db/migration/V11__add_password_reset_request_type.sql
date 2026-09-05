-- =====================================================================
-- TIKZY - V11: Separate password-reset challenges from account unlocks
-- =====================================================================

ALTER TABLE account_unlock_requests
    ADD COLUMN request_type VARCHAR(30) NOT NULL DEFAULT 'ACCOUNT_UNLOCK';

ALTER TABLE account_unlock_requests
    ADD CONSTRAINT chk_account_recovery_request_type
        CHECK (request_type IN ('ACCOUNT_UNLOCK', 'PASSWORD_RESET'));

CREATE INDEX idx_account_recovery_requests_user_type
    ON account_unlock_requests(user_id, request_type, created_at DESC);
