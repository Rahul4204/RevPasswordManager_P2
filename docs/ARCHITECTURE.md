# RevLock — In-Depth Architecture & Layer Documentation

---

## Project Layering Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│  Layer 0 — Entry Point          RevPasswordManagerP2Application      │
├──────────────────────────────────────────────────────────────────────┤
│  Layer 1 — Security             SecurityConfig · JwtFilter           │
│                                 UserDetailsServiceImpl               │
│                                 CustomAuthenticationSuccessHandler   │
├──────────────────────────────────────────────────────────────────────┤
│  Layer 2A — MVC Controllers     AuthController · VaultController     │
│  (Thymeleaf, session-based)     DashboardController · Profile        │
│                                 SecurityController · PwdGenController│
├──────────────────────────────────────────────────────────────────────┤
│  Layer 2B — REST Controllers    AuthRestController                   │
│  (JWT-protected, JSON)          VaultRestController                  │
│                                 PasswordGeneratorRestController      │
├──────────────────────────────────────────────────────────────────────┤
│  Layer 3 — Service              UserService · VaultService           │
│  (Business Logic)               EncryptionService · EmailService     │
│                                 PasswordGeneratorService             │
│                                 SecurityAuditService                 │
│                                 VerificationService                  │
│                                 PasswordRecoveryService              │
├──────────────────────────────────────────────────────────────────────┤
│  Layer 4 — Repository           IUserRepository                      │
│  (Spring Data JPA)              IVaultEntryRepository               │
│                                 ISecurityQuestionRepository          │
│                                 IVerificationCodeRepository          │
├──────────────────────────────────────────────────────────────────────┤
│  Layer 5 — Entity / Domain      User · VaultEntry                    │
│  (JPA Entities)                 SecurityQuestion · VerificationCode  │
├──────────────────────────────────────────────────────────────────────┤
│  Layer 6 — DTO / Mapper         RegisterDTO · VaultEntryDTO          │
│  (Data Transfer)                AuditReport · VaultEntryMapper       │
├──────────────────────────────────────────────────────────────────────┤
│  Layer 7 — Exception            ValidationException                  │
│  (Error Handling)               GlobalExceptionHandler               │
├──────────────────────────────────────────────────────────────────────┤
│  Layer 8 — Util / Filter        JwtUtil · AuthUtil                   │
│  (Cross-Cutting)                JwtAuthenticationFilter              │
├──────────────────────────────────────────────────────────────────────┤
│  Infrastructure                 Oracle DB (JPA/Hibernate)            │
│                                 Gmail SMTP (JavaMailSender)          │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Layer 0 — Entry Point

### `RevPasswordManagerP2Application.java`

```java
@SpringBootApplication
public class RevPasswordManagerP2Application {
    public static void main(String[] args) {
        SpringApplication.run(RevPasswordManagerP2Application.class, args);
    }
}
```

**What it does:**
- `@SpringBootApplication` is a shortcut for three annotations:
  - `@Configuration` — marks this class as a Spring bean source
  - `@EnableAutoConfiguration` — lets Spring Boot auto-configure beans based on the classpath (e.g., auto-configures Spring Security, JPA, Mail)
  - `@ComponentScan` — scans `com.passwordmanager.app` and all sub-packages for `@Component`, `@Service`, `@Repository`, `@Controller` beans
- `SpringApplication.run(...)` bootstraps the embedded Tomcat server, initialises the Spring `ApplicationContext`, and starts serving HTTP requests

**Why it matters:** Everything in the project — security, services, DB connections, mail — is bootstrapped from this single class. If the context fails to start (wrong DB URL, missing config property), the entire application refuses to launch — a "fail fast" safety pattern.

---

## Layer 1 — Security Layer

This is the **front gate** of the application. Every HTTP request passes through this layer before reaching any controller.

### 1a. `SecurityConfig.java`

**Package:** `config`  
**Role:** Defines the Spring Security filter chain — who can access what, how authentication works, and which beans are available to the rest of the app.

**Key decisions made here:**

| Decision | Configuration | Reason |
|---|---|---|
| CSRF disabled | `.csrf(AbstractHttpConfigurer::disable)` | REST endpoints use JWT; stateless clients don't need CSRF tokens |
| Session management | `SessionCreationPolicy.IF_REQUIRED` | Session kept for MVC (Thymeleaf) UI; REST endpoints are stateless |
| Public paths | `/login`, `/register`, `/otp-verify`, `/forgot-password`, `/api/auth/**` | These endpoints must be reachable without a logged-in session |
| Protected paths | Everything else requires `authenticated()` | Forces login before accessing vault, dashboard, profile |
| JWT filter placement | `.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)` | JWT is checked before Spring's own username/password filter |
| Password encoder | `BCryptPasswordEncoder` bean | Used by all services that hash or verify passwords |
| Auth manager | `AuthenticationManager` bean exposed | Used by `AuthRestController` to authenticate REST API login requests |

**Flow for a browser request to `/vault`:**
```
Request → SecurityConfig filter chain
  → Is path public? No
  → Is there a valid HTTP session with authenticated user? Yes → allow through
  → No → redirect to /login
```

**Flow for an API request to `/api/vault`:**
```
Request → JwtAuthenticationFilter (runs first)
  → Extracts Bearer token from Authorization header
  → Validates token with JwtUtil
  → Sets SecurityContext → SecurityConfig checks authenticated() → allow through
```

---

### 1b. `UserDetailsServiceImpl.java`

**Package:** `config`  
**Role:** Bridges the gap between your `User` entity and Spring Security's `UserDetails` interface.

Spring Security doesn't know about your `User` entity — it only works with `UserDetails`. This class teaches Spring Security how to load your users.

```
loadUserByUsername("rahul")
  → calls IUserRepository.findByUsernameOrEmail("rahul", "rahul")
  → wraps User entity in Spring Security's User object
  → returns username, BCrypt hash, and list of granted authorities
```

- If the user is **not found**, throws `UsernameNotFoundException` → Spring Security returns 401
- If the user has **`accountLocked = true`**, the `enabled` / `nonLocked` flags prevent login
- The returned `UserDetails` object is what Spring Security stores in the `SecurityContext` for the duration of the session

---

### 1c. `CustomAuthenticationSuccessHandler.java`

**Package:** `config`  
**Role:** Intercepts the moment a user successfully logs in via the MVC form and decides where to redirect them.

**Logic:**
```
Login success →
  Load User entity from DB
  → Is TOTP (2FA) enabled? Yes  → redirect to /2fa-verify page
                            No  → redirect to /dashboard
```

This is why 2FA works seamlessly — instead of letting Spring Security's default redirect happen, this handler injects a check before the user lands on the dashboard.

---

### 1d. `JwtAuthenticationFilter.java`

**Package:** `filter`  
**Role:** A `OncePerRequestFilter` that validates JWT tokens for every request targeting `/api/*`.

**Step-by-step execution:**

```
Incoming request
  → Does path start with /api/? No → skip filter entirely (pass through)
  → Yes → extract Authorization header
  → Does it start with "Bearer "? No → continue without setting auth (will fail security check)
  → Yes → extract token string
  → JwtUtil.validateToken(token) → is signature valid and not expired?
  → Yes → extract username from token
  → Load UserDetails from UserDetailsServiceImpl
  → Create UsernamePasswordAuthenticationToken
  → Set it in SecurityContextHolder
  → Proceed to controller
```

**Key design choice:** The filter does nothing for non-API paths. MVC routes use session-based auth exclusively. This means the `/vault` page and `/api/vault` endpoint are protected by two completely separate mechanisms.

---

## Layer 2A — MVC Controller Layer (Thymeleaf UI)

These controllers handle browser requests and return HTML pages rendered by Thymeleaf templates. They work with **HTTP sessions** to track the logged-in user.

### `AuthController.java`

**Routes:** `/login`, `/register`, `/otp-verify`, `/forgot-password`, `/reset-password`

**Responsibilities:**
- `GET /login` → renders login form
- `POST /register` → calls `UserService.register()`, saves security questions, sets session, redirects
- `POST /otp-verify` → calls `VerificationService.validateCode()` to confirm registration OTP
- `GET/POST /forgot-password` → loads security questions via `PasswordRecoveryService.getQuestions()`, validates answers
- `POST /reset-password` → calls `PasswordRecoveryService.resetPassword()` after answer validation

**Session management:** After login, Spring Security sets the authenticated principal in the `HttpSession`. The controller reads the current user via `SecurityContextHolder.getContext().getAuthentication()`.

---

### `VaultController.java`

**Routes:** `/vault`, `/vault/add`, `/vault/edit/{id}`, `/vault/delete/{id}`, `/vault/view/{id}`, `/vault/reveal/{id}`

**Responsibilities:**
- Fetches the logged-in user from session
- Calls `VaultService` for all CRUD operations
- Passes search keyword, category filter, and sort order as query parameters to `VaultService.getAllEntries()`
- The **reveal password** flow:
  ```
  POST /vault/reveal/{id}
    → User re-enters master password
    → VaultService.verifyMasterPassword()
    → If correct: VaultService.getEntryWithDecryptedPassword() → show plain text
    → If wrong: show error, password stays masked
  ```

**Security note:** Passwords are **never** shown in the default vault list — only `••••••••` is displayed. Decryption only happens on an explicit reveal action after re-authentication with the master password.

---

### `DashboardController.java`

**Routes:** `/dashboard`

**Responsibilities:**
- Loads summary statistics for the logged-in user:
  - Total vault entries (`IVaultEntryRepository.countByUserId`)
  - Recent entries (`IVaultEntryRepository.findRecentByUserId`)
  - Calls `SecurityAuditService.generateReport()` → gets weak count, reused count, security score
- Puts all values into the `Model` for Thymeleaf rendering

---

### `ProfileController.java`

**Routes:** `/profile`, `/profile/update`, `/profile/change-password`, `/profile/upload-photo`, `/profile/remove-photo`, `/profile/confirm-email`, `/profile/delete-account`

**Responsibilities:**
- Profile update: calls `UserService.updateProfile()` — sets `pending_email` if email is changing
- Email change OTP: generates OTP for the pending email address → user enters code → `UserService.confirmEmailChange()` applies it
- Change password: calls `UserService.changeMasterPassword()` which BCrypt-verifies old password first
- Profile photo: stores Base64-encoded data URI into `User.profilePhotoUrl` (CLOB)
- Delete account: calls `UserService.deleteAccount()` which verifies master password then cascades all data deletion

---

### `SecurityController.java`

**Routes:** `/2fa-setup`, `/2fa-verify`, `/2fa-disable`

**Responsibilities:**
- Toggle TOTP: calls `UserService.toggle2FA()` which generates a 20-byte random Base64 secret and stores it as `totp_secret`
- 2FA verification during login: calls `VerificationService.generateAndSendOtp(user, "2FA")` → saves a `VerificationCode` row → sends email OTP
- `POST /2fa-verify`: calls `VerificationService.validateCode(user, otp, "2FA")` → if valid, logs user into full session

---

### `PasswordGeneratorController.java`

**Routes:** `/generator`

**Responsibilities:**
- `GET /generator` → renders the password generator page with a default `PasswordGeneratorConfigDTO`
- `POST /generator` → receives the config form → calls `PasswordGeneratorService.generate(config)` → displays results with strength scores and labels

---

## Layer 2B — REST Controller Layer (JWT API)

These controllers serve JSON responses to API clients (Postman, mobile apps, JavaScript fetch). They are stateless — no sessions, no HTML. Authentication is via `Authorization: Bearer <token>` header on every request.

### `AuthRestController.java`

**Routes:** `POST /api/auth/login`, `POST /api/auth/register`

**Login flow:**
```
POST /api/auth/login  { "usernameOrEmail": "rahul", "masterPassword": "..." }
  → AuthenticationManager.authenticate(UsernamePasswordAuthenticationToken)
  → Spring Security calls UserDetailsServiceImpl.loadUserByUsername()
  → BCrypt verification happens automatically
  → If success → JwtUtil.generateToken(UserDetails)
  → Return { "token": "eyJhbGc..." }
```

**Register flow:**
```
POST /api/auth/register  { "username": "...", "email": "...", "masterPassword": "..." }
  → UserService.register(RegisterDTO)
  → Returns 201 Created + { "message": "Registration successful" }
```

**Why separate from MVC AuthController?** The MVC controller uses Spring Security's form login (which sets a session cookie). The REST controller uses `AuthenticationManager` programmatically and returns a JWT instead.

---

### `VaultRestController.java`

**Routes:** `GET /api/vault`, `POST /api/vault`, `DELETE /api/vault/{id}`

**How it identifies the caller:** `AuthUtil.getCurrentUser()` reads from the `SecurityContext`, which was populated by `JwtAuthenticationFilter`. No session lookup needed.

```
GET /api/vault
  → AuthUtil.getCurrentUser() → get User entity from SecurityContext
  → VaultService.getAllEntries(userId, search, category, sort)
  → Returns JSON array of VaultEntryDTO (passwords are MASKED)
```

```
POST /api/vault  { "accountName": "Amazon", "password": "MyPass" }
  → VaultService.addEntry(user, dto)
    → EncryptionService.encrypt("MyPass") → "Q3VycmVudA..."
    → Saved to DB as encrypted_password
  → Returns 201 + { "message": "Entry added successfully" }
```

---

### `PasswordGeneratorRestController.java`

**Route:** `POST /api/generator/generate`

```
POST /api/generator/generate  { "length": 16, "count": 3, "includeSymbols": true }
  → PasswordGeneratorService.generate(config)
  → For each generated password: strengthScore() + strengthLabel()
  → Returns [ { "password": "X#9kLm...", "score": 4, "label": "Very Strong" }, ... ]
```

---

## Layer 3 — Service Layer (Business Logic)

The service layer is the **brain of the application**. Controllers are thin — they just parse HTTP input and return HTTP output. All real logic lives here.

### `UserService.java`

The largest service, managing the full user lifecycle.

**`register(RegisterDTO dto)`**
1. Calls `preValidateRegistration()`:
   - Checks `masterPassword == confirmPassword` → throws `ValidationException` if not
   - Checks `existsByUsername()` → throws `ValidationException` if taken
   - Checks `existsByEmail()` → throws `ValidationException` if taken
   - Checks `securityQuestions.size() >= 3` → required for password recovery
2. Builds `User` entity with `passwordEncoder.encode(masterPassword)` → BCrypt hash stored
3. Saves `User` to DB
4. For each security question: `passwordEncoder.encode(answer.toLowerCase().trim())` → saves `SecurityQuestion`

**`changeMasterPassword(userId, ChangePasswordDTO)`**
1. Loads user by ID
2. `verifyMasterPassword(user, dto.getCurrentPassword())` — BCrypt match
3. If wrong → throws `InvalidCredentialsException`
4. Encodes new password → saves

**`toggle2FA(userId, enable)`**
- If `enable=true`: generates 20-byte random → Base64 encode → sets `totp_secret`, sets `totp_enabled=true`
- If `enable=false`: clears `totp_secret=null`, sets `totp_enabled=false`

**`deleteAccount(userId, masterPassword)`**
1. Verifies master password
2. `userRepository.delete(user)` → cascades: `CascadeType.ALL` on `vaultEntries`, `securityQuestions` → all child rows deleted automatically

---

### `VaultService.java`

Manages all encrypted credential storage. **No password is ever saved in plain text.**

**`addEntry(User user, VaultEntryDTO dto)`**
```
dto.getPassword() = "MyRawPassword"
  → enc.encrypt("MyRawPassword") = "iv:ciphertext" (Base64)
  → VaultEntry.encryptedPassword = "iv:ciphertext"
  → vaultRepo.save(VaultEntry)
```

**`getEntryMasked(id, userId)`**
```
findByIdAndUserId(id, userId)  ← ownership check (cannot read another user's entry)
  → VaultEntryDTO.password = "••••••••"  ← never decrypted
  → return DTO
```

**`getEntryWithDecryptedPassword(id, userId)`**
```
findByIdAndUserId(id, userId)
  → enc.decrypt(encryptedPassword) = "MyRawPassword"
  → VaultEntryDTO.password = "MyRawPassword"
  → return DTO  ← only called after master password re-verification
```

**`getAllEntries(userId, search, category, sort)`**
- If `search` is provided → `vaultRepo.search(userId, keyword)` (searches account name, URL, username)
- If `category` is provided → `vaultRepo.findByUserIdAndCategory(userId, category)`
- Sort options: `name` (A-Z), `newest`, `oldest`, `favorites`
- All returned DTOs have masked passwords

---

### `EncryptionService.java`

Handles AES-256-CBC encryption/decryption.

**`encrypt(String plain)`**
1. Loads `secret` key from `@Value("${app.encryption.secret}")`
2. Derives 256-bit AES key from secret bytes
3. Generates random 16-byte IV (Initialization Vector) with `SecureRandom`
4. Encrypts `plain` with AES/CBC/PKCS5Padding
5. Concatenates IV + ciphertext → Base64 encode → returns as a single string

**`decrypt(String cipher)`**
1. Base64 decode the stored string
2. Split first 16 bytes = IV, rest = ciphertext
3. AES/CBC/PKCS5Padding decrypt using same key + extracted IV
4. Returns plain text

**Why AES-CBC with random IV?** Encrypting the same password twice produces different ciphertext each time (because IV is random). This prevents attackers from detecting duplicate passwords by inspecting the DB.

---

### `PasswordGeneratorService.java`

Generates cryptographically random passwords and evaluates their strength.

**`generate(PasswordGeneratorConfigDTO config)`**
- Min length enforced: `Math.max(config.getLength(), 8)`
- Character pool built from flags: uppercase + lowercase (always) + digits (if enabled) + symbols (if enabled)
- Uses `SecureRandom` for selection (not `Math.random()` — it's cryptographically secure)
- Returns a `List<String>` of `config.getCount()` passwords

**`strengthScore(String password)`** → returns 0–4:
- +1 if length ≥ 8
- +1 if contains digits
- +1 if contains special characters
- +1 if length ≥ 12
- Score 0 for empty string

**`strengthLabel(int score)`**:
- 0–1 → `"Weak"`, 2 → `"Medium"`, 3 → `"Strong"`, 4 → `"Very Strong"`

---

### `SecurityAuditService.java`

Analyses a user's entire vault and produces a security report.

**`generateReport(Long userId, int score)`**
1. Load all vault entries: `vaultRepo.findByUserIdOrderByAccountNameAsc(userId)`
2. Decrypt each password: `enc.decrypt(entry.encryptedPassword)`
3. **Weak password detection:** `gen.strengthScore(plain) <= 2` → add to `weakPasswords` list
4. **Reused password detection:** Group by decrypted value → any group with size > 1 → all entries in that group go to `reusedPasswords` list
5. Build `AuditReport` with the lists + `securityScore` (passed from controller, calculated from vault data)

**Why the score is passed in, not calculated here?** The security score depends on how many entries are weak/reused out of the total — the controller calculates it as a percentage before calling this method.

---

### `VerificationService.java`

Manages OTP generation, persistence, and validation.

**`generateAndSendOtp(User user, String purpose)`**
1. Calls `generateCode(user, purpose)`:
   - Creates 6-digit code: `String.format("%06d", secureRandom.nextInt(999999))`
   - Saves `VerificationCode` to DB with `expiresAt = now + expiryMinutes`
2. Calls `emailService.sendOtp(user.getEmail(), code, purpose)` → sends email

**`sendRegistrationOtp(String email)`**
- Generates code and sends email **but does NOT save to DB**
- Used only for registration confirmation where no `User` entity exists yet

**`validateCode(User user, String code, String purpose)`**
1. `codeRepo.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(userId, purpose)` → gets latest unused code
2. Calls `vc.isValid()` → returns `!used && !isExpired()`
3. If valid and code matches → sets `vc.setUsed(true)` → saves → returns `true`
4. If code is expired or wrong → returns `false`

**Anti-replay design:** Once a code is used (`used=true`), it can never be used again. Old codes are cleaned up by `deleteExpiredAndUsed()`.

---

### `PasswordRecoveryService.java`

Handles the password recovery flow without requiring the old password.

**`getQuestions(String usernameOrEmail)`**
- Finds user → returns their `SecurityQuestion` list
- Questions shown to user; answers are NOT shown (only question text)

**`validateAnswers(Long userId, List<String> answers)`**
- Loads questions in order
- For each answer: `passwordEncoder.matches(answer, question.answerHash)` → BCrypt verify
- All answers must match → returns `true`
- One wrong answer → returns `false`

**`resetPassword(String usernameOrEmail, String newPassword)`**
- Only called after `validateAnswers` succeeds
- `passwordEncoder.encode(newPassword)` → updates `masterPasswordHash` → saves

---

### `EmailService.java`

Sends HTML emails via JavaMailSender (Gmail SMTP).

**`sendOtp(String to, String otp, String purpose)`**
- Creates `MimeMessage` → `MimeMessageHelper` for HTML support
- Sets subject and body based on `purpose`:
  - `REGISTRATION` → "Your RevLock registration code"
  - `2FA` / `LOGIN_2FA` → "Your RevLock 2FA code"
- Sends via `JavaMailSender.send(MimeMessage)` → routes to `smtp.gmail.com:587` over TLS

---

## Layer 4 — Repository Layer (Spring Data JPA)

Repositories are **interfaces only** — no implementation code needed. Spring Data JPA generates the SQL queries automatically from the method names at startup.

### `IUserRepository`

| Method | Generated SQL / Query |
|---|---|
| `findByUsername(username)` | `SELECT * FROM pm_users WHERE username = ?` |
| `findByEmail(email)` | `SELECT * FROM pm_users WHERE email = ?` |
| `existsByUsername(username)` | `SELECT COUNT(*) > 0 FROM pm_users WHERE username = ?` |
| `existsByEmail(email)` | same for email |
| `findByUsernameOrEmail(u, e)` | `WHERE username = ? OR email = ?` |
| `save(user)` | `INSERT` or `UPDATE` based on whether `id` is set |
| `delete(user)` | `DELETE WHERE id = ?` → cascades to children |

---

### `IVaultEntryRepository`

| Method | Purpose |
|---|---|
| `findByUserIdOrderByAccountNameAsc(userId)` | All entries, sorted A-Z |
| `findByUserIdAndFavoriteTrueOrderByAccountNameAsc(userId)` | Only favourites |
| `findByUserIdAndCategory(userId, category)` | Category filter |
| `search(userId, keyword)` | `@Query` — custom JPQL searching accountName, websiteUrl, accountUsername |
| `findByIdAndUserId(id, userId)` | Ownership-safe single entry lookup |
| `countByUserId(userId)` | Dashboard total count |
| `findRecentByUserId(userId, pageable)` | `@Query` — most recently added N entries |

**Ownership enforcement:** Every single query includes both `id` AND `userId`. A user can never access another user's vault entry, even if they guess the entry ID.

---

### `ISecurityQuestionRepository`

| Method | Purpose |
|---|---|
| `findByUserId(userId)` | Load questions for recovery / validation |
| `countByUserId(userId)` | Check minimum 3 questions exist |
| `deleteByUserId(userId)` | Called during account deletion |
| `saveAll(questions)` | Bulk-save all 3+ questions during registration |

---

### `IVerificationCodeRepository`

| Method | Purpose |
|---|---|
| `findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(userId, purpose)` | Gets the most recent unused code for a given purpose |
| `deleteExpiredAndUsed(now)` | `@Modifying @Query` — purges expired or used codes |

The `findTop...` method name is parsed by Spring Data into: "Find the top 1 result where userId = ? AND purpose = ? AND used = false, ordered by createdAt DESC" — equivalent to:
```sql
SELECT * FROM pm_verification_codes
WHERE user_id = ? AND purpose = ? AND used = 0
ORDER BY created_at DESC FETCH FIRST 1 ROWS ONLY
```

---

## Layer 5 — Entity / Domain Layer

JPA entities are Java classes that map directly to database tables. Hibernate translates all operations on these objects into SQL.

### `User.java` → table `pm_users`

All fields with their Java type ↔ DB column mapping:

| Java Field | DB Column | Type | Notes |
|---|---|---|---|
| `Long id` | `id` | `NUMBER(19)` | `@SequenceGenerator` (pm_user_seq) |
| `String username` | `username` | `VARCHAR2(50)` | `UNIQUE`, `NOT NULL` |
| `String email` | `email` | `VARCHAR2(100)` | `UNIQUE`, `NOT NULL` |
| `String fullName` | `full_name` | `VARCHAR2(100)` | nullable |
| `String phone` | `phone` | `VARCHAR2(20)` | nullable |
| `String masterPasswordHash` | `master_password_hash` | `VARCHAR2(255)` | BCrypt |
| `boolean emailVerified` | `email_verified` | `NUMBER(1)` | default 0 |
| `String pendingEmail` | `pending_email` | `VARCHAR2(100)` | null when no change pending |
| `String profilePhotoUrl` | `profile_photo_url` | `CLOB` | Base64 data URI |
| `String totpSecret` | `totp_secret` | `VARCHAR2(100)` | null when 2FA off |
| `boolean totpEnabled` | `totp_enabled` | `NUMBER(1)` | default 0 |
| `boolean accountLocked` | `account_locked` | `NUMBER(1)` | default 0 |
| `LocalDateTime createdAt` | `created_at` | `TIMESTAMP` | `@CreationTimestamp` — set once |
| `LocalDateTime updatedAt` | `updated_at` | `TIMESTAMP` | `@UpdateTimestamp` — updated by trigger |
| `List<VaultEntry> vaultEntries` | — | — | `@OneToMany`, lazy, cascade all |
| `List<SecurityQuestion> securityQuestions` | — | — | `@OneToMany`, lazy, cascade all |

---

### `VaultEntry.java` → table `pm_vault_entries`

| Java Field | DB Column | Notes |
|---|---|---|
| `Long id` | `id` | pm_vault_seq |
| `User user` | `user_id` | `@ManyToOne` FK → pm_users |
| `String accountName` | `account_name` | `NOT NULL` |
| `String websiteUrl` | `website_url` | nullable |
| `String accountUsername` | `account_username` | nullable |
| `String encryptedPassword` | `encrypted_password` | AES-256-CBC + Base64 |
| `Category category` | `category` | `@Enumerated(STRING)`, default `OTHER` |
| `String notes` | `notes` | nullable, max 1000 chars |
| `boolean favorite` | `is_favorite` | default false |
| `LocalDateTime createdAt` | `created_at` | auto |
| `LocalDateTime updatedAt` | `updated_at` | auto |

`Category` enum values: `SOCIAL_MEDIA`, `BANKING`, `EMAIL`, `SHOPPING`, `WORK`, `OTHER`

---

### `SecurityQuestion.java` → table `pm_security_questions`

| Java Field | DB Column | Notes |
|---|---|---|
| `Long id` | `id` | pm_sq_seq |
| `User user` | `user_id` | FK → pm_users |
| `String questionText` | `question_text` | The question string |
| `String answerHash` | `answer_hash` | BCrypt of `answer.toLowerCase().trim()` |

---

### `VerificationCode.java` → table `pm_verification_codes`

| Java Field | DB Column | Notes |
|---|---|---|
| `Long id` | `id` | pm_vc_seq |
| `User user` | `user_id` | FK → pm_users |
| `String code` | `code` | 6-digit numeric string |
| `String purpose` | `purpose` | `2FA` or `LOGIN_2FA` |
| `LocalDateTime expiresAt` | `expires_at` | now + `expiryMinutes` |
| `boolean used` | `used` | set true after first successful use |
| `LocalDateTime createdAt` | `created_at` | auto |

**Domain methods on the entity itself:**
- `isExpired()` → `LocalDateTime.now().isAfter(expiresAt)`
- `isValid()` → `!used && !isExpired()`

---

## Layer 6 — DTO / Mapper Layer

DTOs (Data Transfer Objects) prevent direct exposure of entity internals to HTTP clients. They also carry only the fields needed for a specific operation.

### Key DTOs

| DTO | Used For | Key Fields |
|---|---|---|
| `RegisterDTO` | Registration form/API | `username`, `email`, `masterPassword`, `confirmPassword`, `securityQuestions` |
| `LoginDTO` | REST login | `usernameOrEmail`, `masterPassword` |
| `VaultEntryDTO` | Vault add/edit/view | `accountName`, `websiteUrl`, `password` (masked or plain), `category`, `favorite` |
| `PasswordGeneratorConfigDTO` | Generator form/API | `length`, `count`, `includeUppercase`, `includeDigits`, `includeSymbols` |
| `PasswordResultDTO` | Generator response | `password`, `score`, `label` |
| `AuditReport` | Dashboard security report | `weakPasswords`, `reusedPasswords`, `securityScore` |
| `AuditItem` | Item in audit list | `entryId`, `accountName`, `reason` |
| `ChangePasswordDTO` | Password change form | `currentPassword`, `newPassword`, `confirmNewPassword` |
| `ProfileUpdateDTO` | Profile edit | `fullName`, `phone`, `email` |
| `SecurityQuestionDTO` | Registration security Q | `questionText`, `answer` |

### `VaultEntryMapper.java`

Converts `VaultEntry` entity → `VaultEntryDTO`. Handles:
- Mapping all simple fields
- Setting `password = "••••••••"` by default (plain text is never in the mapper)
- Null-safety for optional fields (`websiteUrl`, `notes`)

**Why not use the entity directly?** If controllers returned entities directly, they would expose `encryptedPassword` (the raw AES ciphertext) in JSON responses. The DTO layer ensures only what is intended is returned.

---

## Layer 7 — Exception Layer

### Custom Exceptions

| Exception | Extends | When Thrown |
|---|---|---|
| `ValidationException` | `RuntimeException` | Password mismatch, duplicate username, < 3 security questions |
| `InvalidCredentialsException` | `RuntimeException` | Wrong master password during change or account deletion |
| `ResourceNotFoundException` | `RuntimeException` | User/entry not found by ID |

All are `RuntimeException` (unchecked) so they bubble up naturally without forced try-catch in services.

### `GlobalExceptionHandler.java`

`@RestControllerAdvice` — catches exceptions thrown across all controllers and converts them to proper HTTP responses.

| Exception Caught | HTTP Status Returned |
|---|---|
| `ValidationException` | `400 Bad Request` |
| `InvalidCredentialsException` | `401 Unauthorized` |
| `ResourceNotFoundException` | `404 Not Found` |
| Any other `Exception` | `500 Internal Server Error` |

Without this, Spring would return its default error page (ugly JSON or HTML). This ensures all API consumers get consistent, meaningful error responses.

---

## Layer 8 — Utility / Cross-Cutting Layer

### `JwtUtil.java`

Handles all JWT token operations. Uses **JJWT library**.

**`generateToken(UserDetails userDetails)`**
```
Builds JWT:
  - subject = username
  - issuedAt = now
  - expiration = now + app.jwt.expiration (configured in ms)
  - sign with HMAC-SHA256 using app.jwt.secret
  → returns base64url-encoded String (header.payload.signature)
```

**`validateToken(String token, UserDetails userDetails)`**
1. Parse token → extract claims (throws if signature invalid or expired)
2. Check `username` from token == `userDetails.getUsername()`
3. Check token is not expired
4. Returns `true` only if all pass

**`extractUsername(String token)`** → reads `sub` claim from JWT payload

---

### `AuthUtil.java`

A thin utility method to get the currently authenticated `User` entity from anywhere in the app.

```java
public User getCurrentUser() {
    String username = SecurityContextHolder.getContext()
                          .getAuthentication().getName();
    return userRepository.findByUsernameOrEmail(username, username)
                         .orElseThrow();
}
```

Used primarily in REST controllers where there is no `HttpSession` — the JWT filter has already set the `SecurityContext`, and this util reads it to load the full `User` entity with the ID needed for all service calls.

---

## How All Layers Work Together — End-to-End Flows

### Flow 1: User adds a vault entry via browser

```
User fills form → POST /vault/add
  Layer 1 (Security): Session validated → authenticated
  Layer 2A (VaultController): extracts User from session, builds VaultEntryDTO from form
  Layer 3 (VaultService.addEntry):
      → Layer 3 (EncryptionService.encrypt): AES-256 encrypt password
      → Layer 4 (IVaultEntryRepository.save): INSERT into pm_vault_entries
  Layer 2A: redirect to /vault with success flash message
```

### Flow 2: API client logs in and reads vault

```
POST /api/auth/login { "usernameOrEmail": "rahul", "masterPassword": "..." }
  Layer 8 (JwtAuthenticationFilter): path /api/* → runs
  Layer 1 (SecurityConfig): /api/auth/login is public → skip auth
  Layer 2B (AuthRestController.login):
      → Layer 1 (AuthenticationManager): triggers UserDetailsServiceImpl + BCrypt
      → Layer 8 (JwtUtil.generateToken): creates JWT
      → Returns { "token": "eyJ..." }

GET /api/vault  Authorization: Bearer eyJ...
  Layer 8 (JwtAuthenticationFilter): validates token → sets SecurityContext
  Layer 1 (SecurityConfig): authenticated → allow
  Layer 2B (VaultRestController.getAllEntries):
      → Layer 8 (AuthUtil.getCurrentUser): loads User from SecurityContext
      → Layer 3 (VaultService.getAllEntries): loads entries, all passwords masked
      → Layer 6 (VaultEntryMapper.toDto): entity → DTO
      → Returns JSON array
```

### Flow 3: Password recovery

```
GET /forgot-password → SecurityController shows security questions
  Layer 4 (ISecurityQuestionRepository.findByUserId): loads question text only
  Layer 2A (AuthController): renders questions form

POST /forgot-password  { answers: ["Fluffy", "London", "Smith"] }
  Layer 3 (PasswordRecoveryService.validateAnswers):
      → For each: passwordEncoder.matches(answer, answerHash)  ← BCrypt
      → All 3 pass → return true
  Layer 2A: redirect to reset-password form

POST /reset-password  { newPassword: "NewSecure@123" }
  Layer 3 (PasswordRecoveryService.resetPassword):
      → passwordEncoder.encode("NewSecure@123")
      → User.masterPasswordHash = BCrypt hash
      → Layer 4 (IUserRepository.save): UPDATE pm_users
  Layer 2A: redirect to /login with success message
```

---

## Summary Table

| Layer | Package | Technology | Role |
|---|---|---|---|
| Entry Point | root | Spring Boot | Bootstrap application context |
| Security | `config`, `filter` | Spring Security 6, JWT | Auth, authorization, session/token |
| MVC Controllers | `controller` | Spring MVC, Thymeleaf | Browser UI, session-based |
| REST Controllers | `rest` | Spring MVC, JSON | API endpoints, JWT-based |
| Services | `service` | Plain Java, Spring `@Service` | Business logic, encryption, OTP |
| Repositories | `repository` | Spring Data JPA | DB access, query generation |
| Entities | `entity` | JPA/Hibernate | DB table mapping |
| DTOs & Mapper | `dto`, `mapper` | Plain Java | Data shaping, security boundary |
| Exceptions | `exception` | Spring `@RestControllerAdvice` | Uniform error responses |
| Utilities | `util` | JJWT, Spring Security | JWT ops, current user resolution |
| Infrastructure | — | Oracle DB, Gmail SMTP | Persistence, email delivery |
