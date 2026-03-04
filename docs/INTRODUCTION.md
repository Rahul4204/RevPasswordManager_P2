# RevLock — Project Introduction Documentation

> **RevLock** is a full-stack Spring Boot password manager built as a secure, self-hosted web application. It manages encrypted vault entries, user identity, OTP-based email verification, TOTP two-factor authentication, password generation, and security auditing.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Security | Spring Security 6 + JWT (stateless REST) + Session (MVC) |
| ORM | Spring Data JPA + Hibernate |
| Database | Oracle (production) |
| Templating | Thymeleaf (server-side rendered UI) |
| Build | Maven |
| Testing | JUnit 5 + Mockito + MockMvc |
| Email | Jakarta Mail (Gmail SMTP/TLS) |
| Encryption | AES-256-CBC (`EncryptionService`) |
| Password Hashing | BCrypt (`PasswordEncoder`) |
| Logging | Log4j2 |

---

## Architecture Overview

RevLock is a layered Spring Boot application structured as follows:

```
Browser / API Client
        │
        ▼
┌─────────────────────────────┐
│     Spring Security Layer   │  ← JWT filter (/api/*), Session (/*)
│  SecurityConfig · JwtFilter │    CustomAuthSuccessHandler (2FA)
└────────────┬────────────────┘
             │
     ┌───────┴────────┐
     │                │
┌────▼─────┐   ┌──────▼──────┐
│   MVC    │   │    REST     │
│Controllers│   │ Controllers │
│(Thymeleaf)│   │  (/api/*)  │
└────┬─────┘   └──────┬──────┘
     └────────┬────────┘
              ▼
     ┌────────────────┐
     │  Service Layer │  ← Business logic, AES encryption, OTP
     └────────┬───────┘
              ▼
     ┌────────────────┐
     │  Repository    │  ← Spring Data JPA interfaces
     │  Layer (JPA)   │
     └────────┬───────┘
              ▼
         Oracle DB
```

---

## Data Model Summary

The application uses **4 JPA entities** mapped to Oracle tables:

| Entity | Table | Purpose |
|---|---|---|
| `User` | `pm_users` | Account identity, BCrypt master password, 2FA, profile |
| `VaultEntry` | `pm_vault_entries` | AES-encrypted password entries with category/favorite |
| `SecurityQuestion` | `pm_security_questions` | BCrypt-hashed answers for password recovery |
| `VerificationCode` | `pm_verification_codes` | 6-digit OTP codes with expiry and purpose tracking |

### Relationships
- **User** `1 ──< VaultEntry` (one user owns many vault entries)
- **User** `1 ──< SecurityQuestion` (one user has many security questions)
- **User** `1 ──< VerificationCode` (one user receives many OTP codes)
- All child collections use `CascadeType.ALL` with `orphanRemoval = true`

---

## Key Features

### 🔐 Authentication & Security
- **Session-based login** for Thymeleaf UI with `HttpSession`
- **JWT authentication** for REST API (`Authorization: Bearer <token>`)
- **TOTP Two-Factor Authentication** (time-based OTP via app)
- **Account lockout** flag on `User` entity
- **BCrypt** for all password and security answer hashing

### 🗄️ Vault Management
- Add, edit, delete, and view password entries
- **AES-256-CBC encryption** for all stored passwords
- Category filter (SOCIAL_MEDIA, BANKING, EMAIL, SHOPPING, WORK, OTHER)
- Favorite entries, full-text search (account name, URL, username)
- Masked display (`••••••••`) by default; reveal only after re-entering master password

### 📧 Email OTP Verification
- Registration email verification
- Email address change OTP confirmation
- 2FA login OTP (when TOTP app not configured)
- All OTPs expire after configurable minutes; marked `used=true` after first use

### 🔑 Password Recovery
- Security questions set at registration (BCrypt-answered)
- Recover account by answering configured questions
- Reset master password after validation

### 📊 Security Audit
- Detects **weak passwords** (strength score ≤ 2)
- Detects **reused passwords** (same decrypted value across entries)
- **Security score** passed from controller; aggregated in `AuditReport` DTO
- Dashboard displays total passwords, weak count, reused count

### ⚙️ Password Generator
- Configurable length (min 8), character sets (upper, lower, digits, symbols)
- Multi-password generation (count parameter)
- Real-time **strength scoring** (0–4) and labels (Weak → Very Strong)
- Available as Thymeleaf page and REST endpoint (`/api/generator/generate`)

---


## Module Structure

```
src/main/java/com/passwordmanager/app/
├── config/          ← SecurityConfig, UserDetailsServiceImpl, CustomAuthSuccessHandler
├── controller/      ← MVC controllers (Auth, Vault, Dashboard, Profile, PasswordGenerator)
├── dto/             ← Data Transfer Objects (RegisterDTO, VaultEntryDTO, AuditReport, ...)
├── entity/          ← JPA entities (User, VaultEntry, SecurityQuestion, VerificationCode)
├── exception/       ← Custom exceptions + GlobalExceptionHandler
├── filter/          ← JwtAuthenticationFilter
├── mapper/          ← VaultEntryMapper (entity → DTO)
├── repository/      ← Spring Data JPA interfaces (IUserRepository, IVaultEntryRepository, ...)
├── rest/            ← REST controllers (AuthRestController, VaultRestController, PasswordGeneratorRestController)
├── service/         ← Business logic (UserService, VaultService, EncryptionService, ...)
└── util/            ← JwtUtil, AuthUtil
```

---

## Running the Application

```bash
# Clone and build
mvn clean install

# Run (requires Oracle DB + Gmail SMTP configured in application.properties)
mvn spring-boot:run

# Run tests (no DB required — all tests are pure Mockito unit tests)
mvn test
```

**Port:** `http://localhost:8081`

---

## Configuration Properties

Key properties in `src/main/resources/application.properties`:

| Property | Purpose |
|---|---|
| `spring.datasource.url` | Oracle JDBC connection URL |
| `spring.datasource.username/password` | DB credentials |
| `app.encryption.secret` | AES-256 key for vault encryption |
| `spring.mail.host/port/username/password` | Gmail SMTP credentials |
| `app.verification.expiry-minutes` | OTP expiry window |
| `app.jwt.secret` | JWT signing secret |
| `app.jwt.expiration` | JWT token TTL (ms) |
