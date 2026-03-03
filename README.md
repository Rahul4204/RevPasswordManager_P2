# RevPassManager

RevPassManager is a secure, monolithic Java Spring Boot password manager web application. It features a robust 3-layer architecture (Controller, Service, Repository) providing both a server-rendered Thymeleaf web interface and a comprehensive, stateless JSON REST API secured by JWTs. 

This project allows users to securely register, login, configure security questions and 2FA, and securely manage their personal vault of encrypted passwords. 

## Core Features
1. **Secure Authentication:** Master passwords are encrypted with BCrypt (`BCryptPasswordEncoder`).
2. **Dual-Mode Access:** 
   - Stateful JSESSIONID-based authentication for standard Web browser sessions.
   - Stateless JSON Web Token (JWT) Bearer token authentication for REST APIs API endpoints (`/api/**`).
3. **Data Security:** AES encryption (symmetric key) ensures all vault passwords are secure at rest in the database. 
4. **Password Generator:** Integrated highly-customizable secure password creation tools (REST & Web).
5. **Multi-Factor Security:** Time-based One Time Password (TOTP) 2FA support and secondary security question validation mechanisms.

## Technologies Used
* **Backend:** Java 17, Spring Boot 3.4.3
* **Security:** Spring Security 6, JWT (io.jsonwebtoken), BCrypt
* **Database:** Oracle Database (via `ojdbc11`), Spring Data JPA, Hibernate, HikariCP
* **Frontend:** HTML5, CSS3, Thymeleaf Templates
* **Containerization:** Docker (Multi-stage Eclipse Temurin builds)
* **Testing:** JUnit 4, Mockito, Spring Boot Test, MockMvc
* **Build Tool:** Maven

## Project Structure
The source code follows a standard layered architecture separated by domain and responsibility:

* `src/main/java/com/passwordmanager/app` 
    * `config/` - System configuration files (`SecurityConfig`, etc.)
    * `controller/` - MVC Web Controllers for serving HTML templates.
    * `rest/` - REST API Controllers for handling JSON endpoints (`AuthRestController`, `VaultRestController`).
    * `service/` - Business logic and encryption processing.
    * `repository/` - Spring Data JPA DAO interfaces.
    * `entity/` - Core JPA models (Entities mapped to DB Tables).
    * `dto/` - Data Transfer Objects dictating application ingest boundaries.
    * `mapper/` - Domain component transformers linking DTOs to Entities.
    * `filter/` - Specialized HTTP Request interceptors like `JwtAuthenticationFilter`.
    * `util/` - Cross-cutting utilities (e.g., `JwtUtil`, `AuthUtil`).

* `src/main/resources`
    * `application.properties` - Application database connection & environments configurations.
    * `templates/` - Thymeleaf HTML views.
    * `static/` - Static rendering assets (CSS, JS, Images).

## Requirements
* Java 17 JDK
* Maven (or strictly use the provided `./mvnw` wrapper)
* Oracle Database Instance running locally or remotely (Update `application.properties` credentials)
* Docker (Optional, if using containarized execution)

## Setup & Execution

### 1. Build the application via Maven
```bash
# Clean, verify, and package the jar, skipping testing if necessary.
./mvnw clean compile package -DskipTests
```

### 2. Run Tests
Ensure all unit and MockMvc tests are functional:
```bash
./mvnw clean test
```

### 3. Run the application
```bash
./mvnw spring-boot:run
```
The web interface will natively bind to `http://localhost:8080`. 

### 4. Running via Docker
If you prefer running the containerized build instead:
```bash
# Build the image locally
docker build -t revpassmanager:latest .

# Run the container mapping host port 8080 to container port 8080
docker run -p 8080:8080 revpassmanager:latest
```

## REST API Postman Testing Guide

You can interface directly with the REST layer via Postman or cURL. The API uses a stateless JWT Bearer token authentication mechanism.

BASE URL: `http://localhost:8080`

### 1. Authentication (`/api/auth`)

**A. Register a New User**
* **Method:** `POST`
* **URL:** `/api/auth/register`
* **Headers:** `Content-Type: application/json`
* **Body (raw JSON):**
  ```json
  {
    "username": "api_user",
    "email": "api@example.com",
    "fullName": "API User",
    "masterPassword": "SecurePassword123!",
    "confirmPassword": "SecurePassword123!",
    "securityQuestions": [
      { "questionText": "First pet?", "answer": "dog" },
      { "questionText": "Favorite color?", "answer": "blue" },
      { "questionText": "Mother's maiden name?", "answer": "smith" }
    ]
  }
  ```

**B. Login & Get JWT Token**
* **Method:** `POST`
* **URL:** `/api/auth/login`
* **Headers:** `Content-Type: application/json`
* **Body (raw JSON):**
  ```json
  {
    "usernameOrEmail": "api_user",
    "masterPassword": "SecurePassword123!"
  }
  ```
* **Note:** Save the `token` string returned in the response. For all the following requests, add an Authorization header: `Authorization: Bearer <your_token>`

---

### 2. Vault Management (`/api/vault`)
*(Requires Authorization Header)*

**A. Get All Vault Entries**
* **Method:** `GET`
* **URL:** `/api/vault`
* **Optional Query Params:** `?search=github & category=WORK & sort=name`

**B. Add a New Vault Entry**
* **Method:** `POST`
* **URL:** `/api/vault`
* **Body (raw JSON):**
  ```json
  {
    "accountName": "Github",
    "url": "https://github.com",
    "username": "api_user",
    "password": "MySuperSecretPassword",
    "category": "WORK",
    "notes": "My primary Github account"
  }
  ```

**C. Update an Existing Vault Entry**
* **Method:** `PUT`
* **URL:** `/api/vault/{id}`
* **Body:** (Same structure as Add Entry, but passing updated fields)

**D. Delete a Vault Entry**
* **Method:** `DELETE`
* **URL:** `/api/vault/{id}`

**E. Toggle Favorite Status**
* **Method:** `POST`
* **URL:** `/api/vault/{id}/favorite`

---

### 3. Password Generator (`/api/generator`)
*(Requires Authorization Header)*

**A. Generate Secure Passwords**
* **Method:** `POST`
* **URL:** `/api/generator/generate`
* **Body (raw JSON):**
  ```json
  {
    "length": 16,
    "includeUppercase": true,
    "includeLowercase": true,
    "includeDigits": true,
    "includeSymbols": true,
    "excludeSimilar": false,
    "count": 5
  }
  ```

*(A helpful testing script `test-api.ps1` is provided in the project root if you operate on Windows)*.


