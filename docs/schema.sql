-- =============================================================================
--  RevLock (RevPasswordManager P2) — Oracle SQL Schema
--  Database: Oracle (tested on Oracle 19c+)
--  Updated : 2026-03-04
-- =============================================================================
--
--  Tables
--  ──────────────────────────────────────────────────────────────
--  pm_users               → User.java
--  pm_vault_entries       → VaultEntry.java
--  pm_security_questions  → SecurityQuestion.java
--  pm_verification_codes  → VerificationCode.java
--
--  Sequences (JPA @SequenceGenerator, allocationSize = 1)
--  ──────────────────────────────────────────────────────────────
--  pm_user_seq  | pm_vault_seq | pm_sq_seq | pm_vc_seq
-- =============================================================================


-- ─────────────────────────────────────────────────────────────────────────────
--  SEQUENCES
--  JPA uses @SequenceGenerator / @GeneratedValue(SEQUENCE) on every entity.
--  allocationSize = 1 means the sequence increments by 1 per row.
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
--  Entity : User.java  (@Table name = "pm_users")
--  Notable: master_password_hash is BCrypt.  profile_photo_url is a base64
--           data URI stored as CLOB.  totp_secret is a Base64-encoded 20-byte
--           random secret used to generate 6-digit TOTP codes.
--           pending_email is set during an in-progress email-change flow and
--           cleared (set to NULL) once the OTP is confirmed.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE pm_users (
    id                   NUMBER(19)    NOT NULL,
    username             VARCHAR2(50)  NOT NULL,
    email                VARCHAR2(100) NOT NULL,
    full_name            VARCHAR2(100),
    phone                VARCHAR2(20),
    master_password_hash VARCHAR2(255) NOT NULL,
    email_verified       NUMBER(1)     DEFAULT 0   NOT NULL,
    pending_email        VARCHAR2(100),
    profile_photo_url    CLOB,
    totp_secret          VARCHAR2(100),
    totp_enabled         NUMBER(1)     DEFAULT 0   NOT NULL,
    account_locked       NUMBER(1)     DEFAULT 0   NOT NULL,
    created_at           TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP,
    --
    CONSTRAINT pk_users        PRIMARY KEY (id),
    CONSTRAINT uq_username     UNIQUE      (username),
    CONSTRAINT uq_email        UNIQUE      (email),
    CONSTRAINT chk_email_ver   CHECK       (email_verified   IN (0, 1)),
    CONSTRAINT chk_totp_en     CHECK       (totp_enabled     IN (0, 1)),
    CONSTRAINT chk_acc_locked  CHECK       (account_locked   IN (0, 1))
);

-- Auto-set updated_at on every UPDATE (mirrors @UpdateTimestamp)
CREATE OR REPLACE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON pm_users
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

COMMENT ON TABLE  pm_users                     IS 'Registered user accounts — one row per user';
COMMENT ON COLUMN pm_users.master_password_hash IS 'BCrypt-hashed master password; plain text is NEVER stored';
COMMENT ON COLUMN pm_users.email_verified       IS '1 = email confirmed via OTP; 0 = pending verification';
COMMENT ON COLUMN pm_users.pending_email        IS 'Proposed new e-mail awaiting OTP confirmation; NULL when no change is in progress';
COMMENT ON COLUMN pm_users.profile_photo_url    IS 'Base64-encoded data URI (e.g. data:image/png;base64,...) stored as CLOB';
COMMENT ON COLUMN pm_users.totp_secret          IS 'Base64-encoded 20-byte random secret used to derive TOTP codes (set when 2FA is enabled, NULLed when disabled)';
COMMENT ON COLUMN pm_users.totp_enabled         IS '1 = TOTP two-factor authentication is active for this account';
COMMENT ON COLUMN pm_users.account_locked       IS '1 = login is blocked for this account';


-- ─────────────────────────────────────────────────────────────────────────────
--  TABLE: pm_vault_entries
--  Entity : VaultEntry.java  (@Table name = "pm_vault_entries")
--  Notable: encrypted_password is AES-256-CBC ciphertext, Base64-encoded.
--           category is stored as its enum name (VARCHAR).
--           is_favorite drives the "Favourites" filter in the vault view.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE pm_vault_entries (
    id                 NUMBER(19)    NOT NULL,
    user_id            NUMBER(19)    NOT NULL,
    account_name       VARCHAR2(100) NOT NULL,
    website_url        VARCHAR2(255),
    account_username   VARCHAR2(100),
    encrypted_password VARCHAR2(500) NOT NULL,
    category           VARCHAR2(30)  DEFAULT 'OTHER',
    notes              VARCHAR2(1000),
    is_favorite        NUMBER(1)     DEFAULT 0 NOT NULL,
    created_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP,
    --
    CONSTRAINT pk_vault_entries  PRIMARY KEY (id),
    CONSTRAINT fk_vault_user     FOREIGN KEY (user_id)
                                     REFERENCES pm_users(id)
                                     ON DELETE CASCADE,
    CONSTRAINT chk_vault_cat     CHECK (category IN (
                                     'SOCIAL_MEDIA', 'BANKING', 'EMAIL',
                                     'SHOPPING', 'WORK', 'OTHER'
                                 )),
    CONSTRAINT chk_fav           CHECK (is_favorite IN (0, 1))
);

CREATE OR REPLACE TRIGGER trg_vault_updated_at
    BEFORE UPDATE ON pm_vault_entries
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- User-scoped lookup (almost every vault query filters by user_id)
CREATE INDEX idx_vault_user_id       ON pm_vault_entries (user_id);
-- Speeds up category filter queries
CREATE INDEX idx_vault_user_category ON pm_vault_entries (user_id, category);
-- Speeds up favourite filter queries
CREATE INDEX idx_vault_user_fav      ON pm_vault_entries (user_id, is_favorite);

COMMENT ON TABLE  pm_vault_entries                   IS 'AES-256-encrypted credential entries, one row per saved account';
COMMENT ON COLUMN pm_vault_entries.encrypted_password IS 'AES-256-CBC + Base64: EncryptionService encrypts before saving, decrypts on demand';
COMMENT ON COLUMN pm_vault_entries.category           IS 'VaultEntry.Category enum: SOCIAL_MEDIA | BANKING | EMAIL | SHOPPING | WORK | OTHER';
COMMENT ON COLUMN pm_vault_entries.is_favorite        IS '1 = marked as favourite; surfaces in the Favourites view';


-- ─────────────────────────────────────────────────────────────────────────────
--  TABLE: pm_security_questions
--  Entity : SecurityQuestion.java  (@Table name = "pm_security_questions")
--  Notable: answer_hash is BCrypt of lowercase-trimmed answer (see UserService).
--           At least 3 questions are required per user (enforced in UserService).
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE pm_security_questions (
    id            NUMBER(19)    NOT NULL,
    user_id       NUMBER(19)    NOT NULL,
    question_text VARCHAR2(255) NOT NULL,
    answer_hash   VARCHAR2(255) NOT NULL,
    --
    CONSTRAINT pk_security_questions PRIMARY KEY (id),
    CONSTRAINT fk_sq_user            FOREIGN KEY (user_id)
                                         REFERENCES pm_users(id)
                                         ON DELETE CASCADE
);

CREATE INDEX idx_sq_user_id ON pm_security_questions (user_id);

COMMENT ON TABLE  pm_security_questions            IS 'Security questions for account recovery; at least 3 required per user';
COMMENT ON COLUMN pm_security_questions.answer_hash IS 'BCrypt hash of answer.toLowerCase().trim() — plain text answer is NEVER stored';


-- ─────────────────────────────────────────────────────────────────────────────
--  TABLE: pm_verification_codes
--  Entity : VerificationCode.java  (@Table name = "pm_verification_codes")
--
--  Purpose values actually stored by the application
--  ──────────────────────────────────────────────────────────────
--  '2FA'       — generated by SecurityController.generateAndSendOtp(..., "2FA")
--               validated  by SecurityController.validateCode(..., "2FA")
--  'LOGIN_2FA' — validated  by AuthController.validateCode(..., "LOGIN_2FA")
--
--  Note: 'REGISTRATION' OTPs are sent via EmailService.sendOtp() but are NOT
--  persisted to this table (sendRegistrationOtp() skips codeRepo.save()).
--  VerificationCode.isValid() = !used && !isExpired()
--  VerificationCode.isExpired() = LocalDateTime.now().isAfter(expiresAt)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE pm_verification_codes (
    id         NUMBER(19)   NOT NULL,
    user_id    NUMBER(19)   NOT NULL,
    code       VARCHAR2(10) NOT NULL,
    purpose    VARCHAR2(50) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    used       NUMBER(1)    DEFAULT 0 NOT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    --
    CONSTRAINT pk_verification_codes PRIMARY KEY (id),
    CONSTRAINT fk_vc_user            FOREIGN KEY (user_id)
                                         REFERENCES pm_users(id)
                                         ON DELETE CASCADE,
    CONSTRAINT chk_vc_used           CHECK (used IN (0, 1)),
    CONSTRAINT chk_vc_purpose        CHECK (purpose IN (
                                         '2FA', 'LOGIN_2FA'
                                     ))
);

-- Drives the repository query: findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc
CREATE INDEX idx_vc_user_purpose ON pm_verification_codes (user_id, purpose, used, created_at DESC);

COMMENT ON TABLE  pm_verification_codes          IS 'One-time OTP codes for 2FA login; expired/used rows purged by deleteExpiredAndUsed()';
COMMENT ON COLUMN pm_verification_codes.code      IS '6-digit numeric OTP generated by SecureRandom';
COMMENT ON COLUMN pm_verification_codes.purpose   IS '2FA (security controller flow) | LOGIN_2FA (auth controller flow)';
COMMENT ON COLUMN pm_verification_codes.expires_at IS 'UTC timestamp after which the code is invalid (configurable via app.verification.expiry-minutes, default 10)';
COMMENT ON COLUMN pm_verification_codes.used       IS '1 = code already consumed; prevents OTP replay attacks';


-- ─────────────────────────────────────────────────────────────────────────────
--  MAINTENANCE — Purge expired / used verification codes
--  Called by IVerificationCodeRepository.deleteExpiredAndUsed(LocalDateTime now)
--  via JPQL: DELETE FROM VerificationCode vc WHERE vc.expiresAt < :now OR vc.used = true
-- ─────────────────────────────────────────────────────────────────────────────
-- DELETE FROM pm_verification_codes WHERE used = 1 OR expires_at < CURRENT_TIMESTAMP;


-- ─────────────────────────────────────────────────────────────────────────────
--  END OF SCHEMA
-- ─────────────────────────────────────────────────────────────────────────────
