ALTER TABLE user_accounts
    ADD COLUMN preferred_language VARCHAR(5);

ALTER TABLE user_accounts
    ADD CONSTRAINT chk_user_preferred_language
        CHECK (preferred_language IS NULL OR preferred_language ~ '^[a-z]{2}$');
