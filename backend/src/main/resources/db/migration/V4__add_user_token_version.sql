-- =====================================================================
-- TIKZY - V4: Persist access-token version for logout-all
-- =====================================================================

ALTER TABLE users
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
