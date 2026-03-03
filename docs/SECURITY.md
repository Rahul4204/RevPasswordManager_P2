# Security Document — RevLock (RevPasswordManager P2)

## 1. Authentication

### 1.1 Session-Based (Web UI — Primary)

| Property | Detail |
|---|---|
| Mechanism | Spring Security Form Login |
| Login endpoint | `POST /login` |
| Session store | Server-side `HttpSession` (managed by Spring Security) |
| Username field | `usernameOrEmail` (accepts either) |
| Password field | `masterPassword` |
| Password hashing | BCrypt (adaptive, auto-salted) |
| Wrong credentials | Redirect to `/login?error=true` |
| Logout | `POST /logout` — invalidates session + clears SecurityContextHolder |

### 1.2 JWT (REST API — Secondary)

| Property | Detail |
|---|---|
| Mechanism | Bearer token in `Authorization` header |
| Token format | HMAC-SHA256 signed JWT (JJWT 0.11.5) |
| Token expiry | 10 hours |
| Secret key | Generated in-memory at startup via `Keys.secretKeyFor(HS256)` |
| Scope | Only active on `/api/**` routes |
| Validation | `JwtAuthenticationFilter` validates signature + expiry on every `/api/` request |

---

## 2. Password Security

### 2.1 Master Password (User Login Password)

| Property | Detail |
|---|---|
| Algorithm | BCrypt |
| Salt | Randomly generated per hash by BCrypt |
| Storage | Only the hash is stored in `pm_users.master_password_hash` |
| Plain text | Never stored, never logged |
| Verification | `BCryptPasswordEncoder.matches(rawPassword, storedHash)` |

### 2.2 Vault Passwords (Stored Credentials)

| Property | Detail |
|---|---|
| Algorithm | AES-256-CBC |
| Key derivation | SHA-256 hash of the configured secret string → 256-bit key |
| IV | 16-byte zero array (static IV; suitable for academic project) |
| Encoding | AES ciphertext stored as Base64 string in `encrypted_password` column |
| Storage | Encrypted form only stored in DB — never plain text |
| Decryption | Only happens when user explicitly requests (Reveal Password, Edit, Export) |

### 2.3 Security Question Answers

| Property | Detail |
|---|---|
| Algorithm | BCrypt (same as master password) |
| Normalisation | Lowercased + trimmed before hashing |
| Storage | Only the hash stored in `pm_security_questions.answer_hash` |

---

## 3. Two-Factor Authentication (2FA)

| Property | Detail |
|---|---|
| Type | Email OTP (Time-limited code) |
| Trigger | Login when `totp_enabled = true` on the user account |
| Flow | Login succeeds → `CustomAuthenticationSuccessHandler` clears SecurityContext → stores `pending2faUserId` in session → redirects to `/auth/2fa-login` |
| Code stored in | `pm_verification_codes` table with purpose = `LOGIN_2FA` |
| Expiry | Configurable (default in `VerificationService`) |
| Replay protection | `used` flag — code marked used after first successful validation |
| Finalize login | Valid OTP → manually create `UsernamePasswordAuthenticationToken` → set in `SecurityContextHolder` |

---

## 4. URL Authorization Rules

| URL Pattern | Access |
|---|---|
| `/`, `/login`, `/register` | Public |
| `/recover`, `/recover/**` | Public |
| `/auth/**` | Public |
| `/css/**`, `/js/**`, `/images/**`, `/webjars/**` | Public (static assets) |
| `/api/auth/**` | Public (JWT login/register) |
| **All other URLs** | 🔒 Requires authenticated session |

---

## 5. CSRF Protection

| Property | Detail |
|---|---|
| Status | **Disabled** (`csrf(AbstractHttpConfigurer::disable)`) |
| Reason | REST API endpoints use JWT Bearer tokens (not session cookies), which are not CSRF-vulnerable. Web UI endpoints are protected by the session-based auth requirement. |

---

## 6. OTP / Email Verification

| Purpose | When Used |
|---|---|
| `EMAIL_VERIFY` | During registration — confirms user owns the email before account creation |
| `LOGIN_2FA` | 2FA login step |
| `2FA` | Enabling 2FA on the account |
| `EMAIL_CHANGE` | Profile page email update — confirms new email before applying change |

OTP codes are stored in `pm_verification_codes` with:
- **Expiry timestamp** — `isExpired()` checks `LocalDateTime.now().isAfter(expiresAt)`
- **Used flag** — `isValid()` requires both `!used` AND `!isExpired()`

---

## 7. Master Password Gate

Any sensitive vault operation requires the user to re-enter their **master password** in the UI:

| Operation | Master Password Required |
|---|---|
| Add vault entry | ✅ Yes |
| Edit vault entry | ✅ Yes |
| Delete vault entry | ✅ Yes |
| Reveal stored password | ✅ Yes |
| Change master password | ✅ Yes (current password) |
| Disable 2FA | ✅ Yes |
| Delete account | ✅ Yes |
| Update security questions | ✅ Yes |

---

## 8. Account Lockout

| Property | Detail |
|---|---|
| Field | `account_locked` on `pm_users` |
| Current usage | Field exists and is checked; automated lockout after N failed attempts is extensible |

---

## 9. Session Management

| Property | Detail |
|---|---|
| Type | Server-side `HttpSession` |
| Created on | Successful login |
| Destroyed on | `POST /logout` or explicit `session.invalidate()` (account delete) |
| Expired sessions | Redirect to `/login?expired=true` |
| 2FA pending state | `pending2faUserId` stored in session temporarily until OTP is verified |

---

## 10. Input Validation

| Layer | Mechanism |
|---|---|
| DTO-level | Bean Validation (`@NotBlank`, `@Email`, `@Size`, etc.) via `@Valid` on controller methods |
| Service-level | `preValidateRegistration()` — duplicate username/email checks, password match, min 3 security questions |
| DB-level | `NOT NULL`, `UNIQUE`, `CHECK` constraints in Oracle schema |

---

## 11. Logging & Audit

| Logger | What is Logged |
|---|---|
| `AuthController` | Registration attempts, OTP sends, validation failures |
| `VaultController` | Vault operations at DEBUG level |
| `DashboardController` | Dashboard load at DEBUG level |
| `SecurityAuditService` | Audit report results — weak/reused/old counts and security score |
| **Never logged** | Plain-text passwords, OTP codes, encryption keys |

Log framework: **Log4j2** (Logback excluded from classpath).
