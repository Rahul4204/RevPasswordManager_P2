# Testing Documentation — RevLock (RevPasswordManager P2)

## Overview

The project uses **JUnit 5 + Mockito** for unit testing across all layers.
`MockMvc` (standalone setup) is used for REST controller tests — no Spring context loaded.

**Total test classes: 16**
**Total tests: 84**

---

## Test Inventory

### Application Bootstrap

| Test Class | Tests | What It Tests |
|---|---|---|
| `RevPasswordManagerP2ApplicationTest` | 2 | Application class instantiation; `@SpringBootApplication` annotation presence |

**Test methods:**
- `testApplicationClassExists` — asserts `RevPasswordManagerP2Application` can be instantiated
- `testMainClassAnnotationsPresent` — asserts `@SpringBootApplication` is present on the main class

---

### Service Layer

> **Approach:** Pure Mockito (`@ExtendWith(MockitoExtension.class)`) — no Spring context. Dependencies injected via constructor in `@BeforeEach`.

| Test Class | Tests | What It Tests |
|---|---|---|
| `EmailServiceTest` | 1 | `sendOtp()` invokes `JavaMailSender.createMimeMessage()` and `send()` |
| `EncryptionServiceTest` | 2 | AES encrypt/decrypt round-trip; invalid ciphertext throws `RuntimeException` |
| `PasswordGeneratorServiceTest` | 5 | Default generation (length=16, mixed chars); multi-count; minimum length enforcement; `strengthScore()`; `strengthLabel()` |
| `PasswordRecoveryServiceTest` | 3 | `getQuestions()`; `validateAnswers()` (correct match); `resetPassword()` (hashes and saves) |
| `SecurityAuditServiceTest` | 2 | `generateReport()` — detects weak passwords; detects reused passwords |
| `UserServiceTest` | 4 | `verifyMasterPassword()` (BCrypt match); `register()` — password mismatch throws; duplicate username throws; `findByUsernameOrEmail()` |
| `VaultServiceTest` | 3 | `addEntry()` — encrypts password before save; `getEntryMasked()` — returns `••••••••`; `getEntryWithDecryptedPassword()` — decrypts on reveal |
| `VerificationServiceTest` | 3 | `sendRegistrationOtp()` — returns 6-digit code, invokes email; `validateCode()` — success path (marks used); `validateCode()` — expired code returns false |

#### Test Method Details

**`EmailServiceTest`**
- `testSendOtp` — calls `sendOtp("u@t.com", "123456", "REGISTRATION")`, verifies `createMimeMessage()` and `send(MimeMessage)` are invoked

**`EncryptionServiceTest`**
- `testEncryptDecrypt` — encrypts `"MySecretPassword123"`, asserts ciphertext ≠ plaintext, decrypts back to original
- `testDecryptInvalidText` — asserts `RuntimeException` thrown for `"invalid-base64-text"`

**`PasswordGeneratorServiceTest`**
- `testGenerateDefault` — default config produces 1 password of length 16 with uppercase, lowercase, and digits
- `testGenerateMultiple` — `count=5` produces exactly 5 passwords
- `testMinimumLength` — `length=4` is clamped to minimum of 8
- `testStrengthScore` — `"ComplexP@ssw0rd123!"` → score 4; empty string → score 0
- `testStrengthLabel` — score 0→"Weak", 2→"Medium", 3→"Strong", 4→"Very Strong"

**`PasswordRecoveryServiceTest`**
- `testGetQuestions` — mocked user found by username; returns list of `SecurityQuestion`
- `testValidateAnswers` — BCrypt `matches()` returns true → method returns `true`
- `testResetPassword` — encodes new password, sets `masterPasswordHash`, saves user

**`SecurityAuditServiceTest`**
- `testWeakPassword` — single entry with `strengthScore=1` appears in `weakPasswords`; `securityScore=90`
- `testReusedPassword` — two entries decrypting to same plaintext both appear in `reusedPasswords`

**`UserServiceTest`**
- `testVerifyMasterPassword` — `encoder.matches("raw","h")` true → returns true
- `testRegisterPasswordsMismatch` — `masterPassword ≠ confirmPassword` → throws `ValidationException`
- `testRegisterDuplicateUsername` — `existsByUsername` returns true → throws `ValidationException`
- `testFindByUsernameOrEmail` — mocked repo returns user; asserts username matches

**`VaultServiceTest`**
- `testAddEntry` — `enc.encrypt("raw")` called; saved entry has `encryptedPassword="encrypted"`
- `testGetEntryMasked` — returned DTO password field is `"••••••••"`
- `testGetEntryWithDecryptedPassword` — `enc.decrypt("enc")` called; DTO password is `"dec"`

**`VerificationServiceTest`**
- `testSendOtp` — 6-char code returned; `email.sendOtp(to, code, "REGISTRATION")` verified
- `testValidate_Success` — active unused code with future expiry → returns `true`, marks `used=true`
- `testValidate_Expired` — code with past expiry → returns `false`

---

### Repository Layer

> **Approach:** Repository interface is mocked with Mockito — no H2 database or `@DataJpaTest`. Tests verify query method contracts and behavior.

| Test Class | Tests | What It Tests |
|---|---|---|
| `ISecurityQuestionRepositoryTest` | 11 | `findByUserId`, `countByUserId`, `deleteByUserId`, `save`, `saveAll`, `findById`, `delete`, `deleteById` |
| `IUserRepositoryTest` | 12 | `findByUsername`, `findByEmail`, `existsByUsername`, `existsByEmail`, `findByUsernameOrEmail`, `save`, `findAll`, `findById`, `deleteById` |
| `IVaultEntryRepositoryTest` | 16 | `findByUserIdOrderByAccountNameAsc`, `findByUserIdAndFavoriteTrueOrderByAccountNameAsc`, `findByUserIdAndCategory`, `search` (by name/URL/username), `findByIdAndUserId`, `countByUserId`, `findRecentByUserId`, `save`, `deleteById` |
| `IVerificationCodeRepositoryTest` | 14 | `findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc`, `deleteExpiredAndUsed`, `VerificationCode.isValid()`, `isExpired()`, `save`, `findById`, `findAll`, `deleteById` |

#### Notable Test Cases

**`IVaultEntryRepositoryTest`**
- `search_matchesByAccountName` — keyword `"amazon"` returns Amazon entry
- `search_matchesByWebsiteUrl` — keyword `"google"` returns Gmail entry
- `search_matchesByUsername` — keyword `"user123"` returns HDFC Bank entry
- `findByIdAndUserId_wrongUser_empty` — cross-user access returns empty (ownership enforcement)
- `findFavorites_returnsOnlyFavorites` — only entries with `favorite=true` returned

**`IVerificationCodeRepositoryTest`**
- `isValid_activeCode_true` — unexpired + unused → `isValid()` = true
- `isValid_expiredCode_false` — past expiry → `isValid()` = false
- `isValid_usedCode_false` — `used=true` → `isValid()` = false
- `findTop_allUsed_returnsEmpty` — consumed 2FA code not returned as candidate

---

### REST Controller Layer

> **Approach:** `MockMvcBuilders.standaloneSetup()` — no Spring context, no security filter chain. Services mocked with Mockito.

| Test Class | Tests | What It Tests |
|---|---|---|
| `AuthRestControllerTest` | 2 | `POST /api/auth/login` → 200 + JWT token; `POST /api/auth/register` → 201 + success message |
| `VaultRestControllerTest` | 3 | `GET /api/vault` → 200 JSON list; `POST /api/vault` → 201 + message; `DELETE /api/vault/{id}` → 200 + message |
| `PasswordGeneratorRestControllerTest` | 1 | `POST /api/generator/generate` → 200 JSON with `password` and `score` fields |

#### Test Method Details

**`AuthRestControllerTest`**
- `testLogin` — sends `{"usernameOrEmail":"u","masterPassword":"p"}`, asserts `$.token = "tok"`
- `testRegister` — sends valid registration body, asserts HTTP 201 and `$.message = "Registration successful"`

**`VaultRestControllerTest`**
- `testGetAllEntries` — `GET /api/vault` returns `application/json` with status 200
- `testAddEntry` — `POST /api/vault` with `{"accountName":"T","password":"p"}` returns 201 + message
- `testDeleteEntry` — `DELETE /api/vault/1` returns 200 + `"Entry deleted successfully"`

**`PasswordGeneratorRestControllerTest`**
- `testGenerate` — `POST /api/generator/generate` with `{"length":16}`, asserts `$[0].password = "mock-pass"` and `$[0].score = 4`

---

## How to Run Tests

### Run all tests
```bash
mvn test
```

### Run a single test class
```bash
mvn test -Dtest=VaultServiceTest
```

### Run tests matching a pattern
```bash
mvn test -Dtest="*ServiceTest"
```

### Run repository tests only
```bash
mvn test -Dtest="*RepositoryTest"
```

### Generate Surefire HTML report
```bash
mvn surefire-report:report
# Report: target/site/surefire-report.html
```

### Skip tests during build
```bash
mvn package -DskipTests
```

---

## Test Configuration Notes

- All unit tests use **pure Mockito** — no Spring context, no H2 database
- Repository tests mock the JPA interface directly to verify query method contracts
- REST controller tests use `MockMvcBuilders.standaloneSetup()` — no security filter applied
- `ReflectionTestUtils.setField()` used to inject `@Value`-annotated fields (e.g., `secret`, `fromEmail`, `expiryMinutes`)
- `@ExtendWith(MockitoExtension.class)` used on all test classes requiring mocks

---

## Test Results Location

| Artifact | Path |
|---|---|
| Surefire XML reports | `target/surefire-reports/*.xml` |
| Surefire HTML report | `target/site/surefire-report.html` |
| Console output | Terminal / CI build log |

---

## Logging & Auditing Verification

Application logs (INFO, DEBUG, WARN, ERROR via Log4j2) are streamed to the console during `mvn test`. This implicitly verifies that Log4j2 is configured correctly and event logging is active across Services and REST Controllers.
