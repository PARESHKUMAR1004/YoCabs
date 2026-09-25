CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    mobile VARCHAR(20),
    email VARCHAR(200),
    password_hash VARCHAR(100),
    role VARCHAR(30) NOT NULL,
    partner_id UUID,
    display_name VARCHAR(200),
    status VARCHAR(20) NOT NULL,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_user_partner
        FOREIGN KEY (partner_id) REFERENCES travel_partners(id)
            ON DELETE RESTRICT,

    CONSTRAINT uq_user_mobile UNIQUE (mobile),
    CONSTRAINT uq_user_email UNIQUE (email),

    CONSTRAINT chk_user_role CHECK (
        role IN ('TOURIST', 'PARTNER_OWNER', 'PARTNER_STAFF',
                 'DRIVER', 'ADMIN', 'SUPER_ADMIN')
    ),
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'BLOCKED')),
    CONSTRAINT chk_user_login_identifier CHECK (
        mobile IS NOT NULL OR email IS NOT NULL
    ),
    CONSTRAINT chk_user_partner_role CHECK (
        (role IN ('PARTNER_OWNER', 'PARTNER_STAFF')) = (partner_id IS NOT NULL)
        OR role = 'DRIVER'
    )
);

CREATE INDEX idx_user_accounts_partner ON user_accounts(partner_id);

CREATE TABLE otp_challenges (
    id UUID PRIMARY KEY,
    mobile VARCHAR(20) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_otp_challenges_mobile ON otp_challenges(mobile, created_at DESC);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES user_accounts(id)
            ON DELETE CASCADE,

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
