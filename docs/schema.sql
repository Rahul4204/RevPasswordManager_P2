-- =============================================================================
--  RevLock (RevPasswordManager P2) — Oracle SQL Schema
--  Database: Oracle SQL
--  Generated: 2026-03-03
-- =============================================================================


-- ─────────────────────────────────────────────────────────────────────────────
--  SEQUENCES  (used by JPA @SequenceGenerator instead of IDENTITY columns)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE SEQUENCE pm_user_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE pm_vault_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE pm_sq_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE pm_vc_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;


-- ─────────────────────────────────────────────────────────────────────────────
--  TABLE: pm_users
--  Maps to entity: User.java
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE pm_users (
    id                   NUMBER(19)      NOT NULL,
    username             VARCHAR2(50)    NOT NULL,
    email                VARCHAR2(100)   NOT NULL,
    full_name            VARCHAR2(100),
    phone                VARCHAR2(20),
    master_password_hash VARCHAR2(255)   NOT NULL,
    email_verified       NUMBER(1)       DEFAULT 0 NOT NULL,  -- 0=false, 1=true
    pending_email        VARCHAR2(100),
    profile_photo_url    CLOB,
    totp_secret          VARCHAR2(100),
    totp_enabled         NUMBER(1)       DEFAULT 0 NOT NULL,
    account_locked       NUMBER(1)       DEFAULT 0 NOT NULL,
    created_at           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP,
    CONSTRAINT pk_users        PRIMARY KEY (id),
    CONSTRAINT uq_username     UNIQUE (username),
    CONSTRAINT uq_email        UNIQUE (email)
);

-- Trigger to auto-set updated_at on every UPDATE
CREATE OR REPLACE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON pm_users
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

COMMENT ON TABLE  pm_users                    IS 'Registered user accounts';
COMMENT ON COLUMN pm_users.master_password_hash IS 'BCrypt hash of the master password — plain text is never stored';
COMMENT ON COLUMN pm_users.profile_photo_url  IS 'Base64-encoded data URL stored as CLOB';
COMMENT ON COLUMN pm_users.totp_secret        IS 'Random Base64 secret used to generate 2FA OTP codes';
COMMENT ON COLUMN pm_users.pending_email      IS 'New email awaiting OTP confirmation; NULL when no change is in progress';


-- ─────────────────────────────────────────────────────────────────────────────
--  TABLE: pm_vault_entries
--  Maps to entity: VaultEntry.java
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE pm_vault_entries (
    id                 NUMBER(19)      NOT NULL,
    user_id            NUMBER(19)      NOT NULL,
    account_name       VARCHAR2(100)   NOT NULL,
    website_url        VARCHAR2(255),
    account_username   VARCHAR2(100),
    encrypted_password VARCHAR2(500)   NOT NULL,
    category           VARCHAR2(30)    DEFAULT 'OTHER',
    notes              VARCHAR2(1000),
    is_favorite        NUMBER(1)       DEFAULT 0 NOT NULL,
    created_at         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP,
    CONSTRAINT pk_vault_entries  PRIMARY KEY (id),
    CONSTRAINT fk_vault_user     FOREIGN KEY (user_id) REFERENCES pm_users(id) ON DELETE CASCADE,
    CONSTRAINT chk_vault_cat     CHECK (category IN (
        'SOCIAL_MEDIA','BANKING','EMAIL','SHOPPING','WORK','OTHER'
    ))
);

CREATE OR REPLACE TRIGGER trg_vault_updated_at
    BEFORE UPDATE ON pm_vault_entries
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- Index for fast user vault lookups (used on almost every query)
CREATE INDEX idx_vault_user_id ON pm_vault_entries (user_id);

COMMENT ON TABLE  pm_vault_entries                  IS 'Encrypted credential entries stored per user';
COMMENT ON COLUMN pm_vault_entries.encrypted_password IS 'AES-256-CBC encrypted, Base64-encoded password ciphertext';
COMMENT ON COLUMN pm_vault_entries.category          IS 'Enum: SOCIAL_MEDIA | BANKING | EMAIL | SHOPPING | WORK | OTHER';


-- ─────────────────────────────────────────────────────────────────────────────
--  TABLE: pm_security_questions
--  Maps to entity: SecurityQuestion.java
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE pm_security_questions (
    id            NUMBER(19)    NOT NULL,
    user_id       NUMBER(19)    NOT NULL,
    question_text VARCHAR2(255) NOT NULL,
    answer_hash   VARCHAR2(255) NOT NULL,
    CONSTRAINT pk_security_questions PRIMARY KEY (id),
    CONSTRAINT fk_sq_user            FOREIGN KEY (user_id) REFERENCES pm_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_sq_user_id ON pm_security_questions (user_id);

COMMENT ON TABLE  pm_security_questions           IS 'Security questions used for password recovery';
COMMENT ON COLUMN pm_security_questions.answer_hash IS 'BCrypt hash of the lowercase-trimmed answer — never stored in plain text';


-- ─────────────────────────────────────────────────────────────────────────────
--  TABLE: pm_verification_codes
--  Maps to entity: VerificationCode.java
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE pm_verification_codes (
    id         NUMBER(19)   NOT NULL,
    user_id    NUMBER(19)   NOT NULL,
    code       VARCHAR2(10) NOT NULL,
    purpose    VARCHAR2(50) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    used       NUMBER(1)    DEFAULT 0 NOT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_verification_codes PRIMARY KEY (id),
    CONSTRAINT fk_vc_user            FOREIGN KEY (user_id) REFERENCES pm_users(id) ON DELETE CASCADE,
    CONSTRAINT chk_vc_purpose        CHECK (purpose IN (
        'EMAIL_VERIFY','LOGIN_2FA','2FA','EMAIL_CHANGE'
    ))
);

CREATE INDEX idx_vc_user_id ON pm_verification_codes (user_id);

COMMENT ON TABLE  pm_verification_codes         IS 'OTP codes for email verification, 2FA login, and email change confirmations';
COMMENT ON COLUMN pm_verification_codes.purpose IS 'EMAIL_VERIFY | LOGIN_2FA | 2FA | EMAIL_CHANGE';
COMMENT ON COLUMN pm_verification_codes.used    IS '1 = code already consumed; prevents replay attacks';


-- ─────────────────────────────────────────────────────────────────────────────
--  OPTIONAL: Clean up expired/used verification codes (run via scheduler)
-- ─────────────────────────────────────────────────────────────────────────────
-- DELETE FROM pm_verification_codes WHERE used = 1 OR expires_at < CURRENT_TIMESTAMP;


-- ─────────────────────────────────────────────────────────────────────────────
--  END OF SCHEMA
-- ─────────────────────────────────────────────────────────────────────────────
