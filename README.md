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

## REST API Documentation

Once running, you can interface directly with the REST layer via Postman or cURL. 
* To get a token, send a **POST** to `/api/auth/login` providing a JSON body with `usernameOrEmail` and `masterPassword`.
* Store the returned `token` string. 
* Pass this token on all subsequent requests to `/api/vault` or `/api/generator` using an HTTP Request Header constraint: `Authorization: Bearer <token_string>` 

*(A helpful testing script `test-api.ps1` is provided in the project root if you operate on Windows)*.

---
**Disclaimer:** This is an academic/portfolio implementation. In a production environment, HTTPS/TLS should be heavily strictly enforced prior to transmission of raw credentials natively.
