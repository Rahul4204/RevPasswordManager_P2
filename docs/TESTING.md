# Testing Artifacts — RevLock (RevPasswordManager P2)

## Overview

The project uses **JUnit 5 + Mockito** for unit testing across all layers.
Spring Test (`@WebMvcTest`, `@SpringBootTest`) is used for integration-level controller tests.

**Total test classes: 27**
**Total tests: 92**

---

## Test Inventory

### Application Bootstrap
| Test Class | What It Tests |
|---|---|
| `RevPasswordManagerP2ApplicationTest` | Spring context loads without errors |

### Config Layer
| Test Class | What It Tests |
|---|---|
| `SecurityConfigTest` | Filter chain beans, password encoder bean, auth manager bean |
| `UserDetailsServiceImplTest` | `loadUserByUsername()` — found/not-found cases |
| `CustomAuthenticationSuccessHandlerTest` | Redirect to dashboard vs 2FA page |

### Filter Layer
| Test Class | What It Tests |
|---|---|
| `JwtAuthenticationFilterTest` | Skips non-`/api/` paths; validates Bearer token; sets SecurityContext |

### Service Layer
| Test Class | What It Tests |
|---|---|
| `UserServiceTest` | register, preValidate, verifyMasterPassword, changeMasterPassword, toggle2FA, deleteAccount |
| `VaultServiceTest` | addEntry (encrypts password), updateEntry, deleteEntry, toggleFavorite, getAllEntries (search/filter/sort), getEntryMasked, getEntryWithDecryptedPassword |
| `EncryptionServiceTest` | encrypt/decrypt round-trip, wrong key fails |
| `PasswordGeneratorServiceTest` | generate() with various config options, strengthScore(), strengthLabel() |
| `SecurityAuditServiceTest` | generateReport() — detects weak, reused, old passwords; security score calculation |
| `VerificationServiceTest` | sendRegistrationOtp(), generateAndSendOtp(), validateCode() — valid/expired/used |
| `PasswordRecoveryServiceTest` | getQuestions(), validateAnswers() — correct/incorrect, resetPassword() |
| `EmailServiceTest` | sendEmail() async invocation via JavaMailSender |

### Controller Layer
| Test Class | What It Tests |
|---|---|
| `AuthControllerTest` | GET /login, GET /register, POST /register (valid/invalid), OTP verify flow, 2FA login flow, password recovery flow |
| `VaultControllerTest` | GET /vault (search/filter), view entry, reveal password (wrong/correct master pw), add/edit/delete entry, toggle favorite |
| `DashboardControllerTest` | GET /dashboard — model attributes (totalPasswords, weakCount, etc.) |
| `ProfileControllerTest` | GET/POST /profile, email change + OTP confirm, remove photo, delete account |
| `PasswordGeneratorControllerTest` | GET /generator page, POST generate with config |

### REST Controller Layer
| Test Class | What It Tests |
|---|---|
| `AuthRestControllerTest` | POST /api/auth/login — valid credentials return JWT; bad credentials return 401 |
| `VaultRestControllerTest` | GET /api/vault, POST /api/vault/add — JWT-authenticated requests |
| `PasswordGeneratorRestControllerTest` | POST /api/generator — JSON request/response |

### Exception Layer
| Test Class | What It Tests |
|---|---|
| `GlobalExceptionHandlerTest` | Spring MVC exception handler resolves to correct HTTP status |
| `ValidationExceptionTest` | Custom exception carries correct message |
| `InvalidCredentialsExceptionTest` | Custom exception carries correct message |
| `ResourceNotFoundExceptionTest` | Custom exception carries correct message |

### Mapper / Util Layer
| Test Class | What It Tests |
|---|---|
| `VaultEntryMapperTest` | `toDto()` mapping — all fields, null safety |
| `AuthUtilTest` | `getCurrentUser()` — authenticated/anonymous principal resolution |

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

### Run with a specific Spring profile
```bash
mvn test -Dspring.profiles.active=test
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

- Tests that touch the database use an **in-memory H2 database** (`spring.datasource.url=jdbc:h2:mem:testdb`)
- `application.properties` for tests is located at `src/test/resources/application.properties`
- Mockito `@Mock` + `@InjectMocks` are used for pure unit tests (no Spring context loaded)
- `@WebMvcTest` is used for controller tests (loads only the web layer, mocks services)
- `@SpringBootTest` is used for full integration context tests

---

## Test Results Location

| Artifact | Path |
|---|---|
| Surefire XML reports | `target/surefire-reports/*.xml` |
| Surefire HTML report | `target/site/surefire-report.html` |
| Console output | Jenkins build log / terminal |

---

## Logging & Auditing Verification

- **Log4j2 Integration**: The test suite implicitly verifies Log4j2 configuration. Application logs (INFO, DEBUG, WARN, ERROR) are streamed to the console during `mvn test` execution. This verifies that event actions across Controllers, Services, and Security Configurations are appropriately logged.
