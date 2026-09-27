-- Where to send a user's notifications when the app is closed: one row per installed app.
-- The token belongs to the phone, so registering it again as someone else moves it.
CREATE TABLE device_tokens (
    token      VARCHAR(200) PRIMARY KEY,
    user_id    UUID NOT NULL,
    platform   VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_device_token_user
        FOREIGN KEY (user_id) REFERENCES user_accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_device_tokens_user ON device_tokens(user_id);
