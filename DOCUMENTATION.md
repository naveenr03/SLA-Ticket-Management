# SLA Ticket Management System — Documentation

## Table of Contents
1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Configuration](#configuration)
5. [Database Schema](#database-schema)
6. [Authentication & Security](#authentication--security)
7. [API Reference](#api-reference)
8. [Error Handling](#error-handling)
9. [Running the Application](#running-the-application)

---

## Overview

**SLA Ticket** is a Spring Boot REST API for managing support tickets with role-based access control, JWT authentication, and SLA tracking capabilities. Users can register, log in, create tickets, and view their own tickets. The app uses PostgreSQL as its database and Flyway for schema migrations.

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 4.0.6 | Framework |
| Spring Security | (managed) | Authentication & Authorization |
| Spring Data JPA / Hibernate | (managed) | ORM & Database Access |
| PostgreSQL | (runtime) | Relational Database |
| Flyway | (managed) | Database Migrations |
| JJWT | 0.12.5 | JWT Token Generation & Validation |
| Lombok | (managed) | Boilerplate Reduction |
| Spring Boot Actuator | (managed) | Health & Metrics Endpoints |
| Jakarta Validation | (managed) | Request Validation |
| Maven | (wrapper) | Build Tool |

---

## Project Structure

```
com.naveen.slaticket
├── SlaticketApplication.java          # Entry point (sets timezone to Asia/Kolkata)
├── auth/
│   ├── controller/AuthController.java # POST /api/auth/register, /api/auth/login
│   ├── dto/
│   │   ├── AuthResponse.java         # { accessToken, tokenType }
│   │   ├── LoginRequest.java         # { email, password }
│   │   └── RegisterRequest.java      # { name, email, password }
│   ├── security/
│   │   ├── CustomUserDetailsService.java  # Loads user from DB by email
│   │   ├── JwtAuthenticationFilter.java   # Extracts & validates JWT from Authorization header
│   │   ├── JwtService.java               # JWT generate/validate/extract utilities
│   │   └── SecurityConfig.java           # Security filter chain, stateless sessions, BCrypt
│   └── service/AuthService.java       # Registration & login business logic
├── common/
│   ├── dto/ApiErrorResponse.java      # Standardized error response DTO
│   ├── entity/BaseEntity.java         # (placeholder, unused)
│   ├── exception/
│   │   ├── BadRequestException.java
│   │   ├── GlobalExceptionHandler.java    # Central @RestControllerAdvice
│   │   ├── ResourceNotFoundException.java # (placeholder)
│   │   └── UnauthorizedException.java     # (placeholder)
│   └── util/SecurityUtils.java        # (placeholder)
├── config/
│   └── JpaAuditingConfig.java         # Enables @CreatedDate / @LastModifiedDate
├── ticket/
│   ├── controller/TicketController.java   # POST /api/tickets, GET /api/tickets/me
│   ├── dto/
│   │   ├── CreateTicketRequest.java   # { title, description, priority }
│   │   ├── PagedResponse.java         # Generic paginated response wrapper
│   │   └── TicketResponse.java        # Ticket output DTO
│   ├── entity/
│   │   ├── Priority.java             # LOW, MEDIUM, HIGH, CRITICAL
│   │   ├── Ticket.java               # JPA entity with auditing
│   │   └── TicketStatus.java         # OPEN, IN_PROGRESS, RESOLVED, CLOSED
│   ├── repository/TicketRepository.java   # findByCreatedBy (paged)
│   └── service/TicketService.java     # Create ticket, get my tickets
└── user/
    ├── controller/UserController.java # GET /api/users/me
    ├── dto/
    │   ├── UserProfileResponse.java   # { id, name, email, role }
    │   └── UserResponse.java          # (placeholder)
    ├── entity/
    │   ├── Role.java                  # ROLE_USER, ROLE_ADMIN, ROLE_MANAGER, ROLE_AGENT
    │   └── User.java                  # JPA entity with @PrePersist/@PreUpdate timestamps
    ├── repository/UserRepository.java # findByEmail, existsByEmail
    └── service/UserService.java       # (placeholder interface)
```

---

## Configuration

**File:** `src/main/resources/application.yaml`

| Property | Value | Description |
|---|---|---|
| `server.port` | 8080 | HTTP port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/slaticket` | PostgreSQL connection |
| `spring.datasource.username` | postgres | DB user |
| `spring.datasource.password` | postgres | DB password |
| `spring.jpa.hibernate.ddl-auto` | validate | Schema validated against entities (Flyway manages DDL) |
| `spring.jpa.open-in-view` | false | Disables OSIV anti-pattern |
| `spring.flyway.enabled` | true | Flyway auto-runs migrations |
| `app.jwt.secret` | (Base64 encoded key) | HMAC signing key for JWT |
| `app.jwt.expiration-ms` | 3600000 | Token validity: 1 hour |
| `management.endpoints.web.exposure.include` | health, info, metrics | Actuator endpoints exposed |

---

## Database Schema

Managed via Flyway migrations in `src/main/resources/db/migration/`.

### V1 — `users` table

```sql
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,
    role        VARCHAR(30)   NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### V2 — `tickets` table

```sql
CREATE TABLE tickets (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(150)  NOT NULL,
    description  TEXT          NOT NULL,
    status       VARCHAR(30)   NOT NULL,
    priority     VARCHAR(30)   NOT NULL,
    created_by   BIGINT        NOT NULL REFERENCES users(id),
    assigned_to  BIGINT        NULL     REFERENCES users(id),
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes on: created_by, assigned_to, status, priority
```

### Entity Relationship

```
User (1) ──── creates ────> (*) Ticket
User (1) ──── assigned ───> (*) Ticket (optional)
```

---

## Authentication & Security

### Flow

1. **Register** → Password is BCrypt-hashed, user saved with `ROLE_USER`, JWT returned.
2. **Login** → Credentials authenticated via `AuthenticationManager` + `DaoAuthenticationProvider`, JWT returned.
3. **Subsequent requests** → `JwtAuthenticationFilter` (runs before `UsernamePasswordAuthenticationFilter`):
   - Extracts `Bearer <token>` from `Authorization` header.
   - Validates token signature & expiry.
   - Loads `UserDetails` from DB and sets `SecurityContext`.

### Security Configuration

- **Stateless sessions** — no server-side session.
- **CSRF disabled** — appropriate for stateless JWT APIs.
- **Public endpoints:** `/api/auth/**`, `/actuator/health`
- **All other endpoints:** require authentication.
- **Method-level security** enabled via `@EnableMethodSecurity`.

### JWT Details

- Algorithm: HMAC-SHA (key from `app.jwt.secret` Base64-decoded)
- Claims: `sub` = user email, `iat`, `exp`
- Expiration: 1 hour

### Roles

| Role | Description |
|---|---|
| `ROLE_USER` | Default role assigned on registration |
| `ROLE_ADMIN` | Administrator (not yet assigned via API) |
| `ROLE_MANAGER` | Manager (not yet assigned via API) |
| `ROLE_AGENT` | Support agent (not yet assigned via API) |

---

## API Reference

### Authentication

#### POST `/api/auth/register`

Register a new user account.

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securepass123"
}
```

**Validation:**
- `name`: required, max 100 chars
- `email`: required, valid email format, max 150 chars
- `password`: required, 8–100 chars

**Response (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJI...",
  "tokenType": "Bearer"
}
```

**Errors:**
- `400` — Validation failed or email already registered.

---

#### POST `/api/auth/login`

Authenticate and obtain a JWT.

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "securepass123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJI...",
  "tokenType": "Bearer"
}
```

**Errors:**
- `401` — Invalid email or password.

---

### User

#### GET `/api/users/me`

Get the authenticated user's profile.

**Headers:** `Authorization: Bearer <token>`

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "ROLE_USER"
}
```

---

### Tickets

#### POST `/api/tickets`

Create a new support ticket.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "title": "Login page broken",
  "description": "Unable to access login page since morning.",
  "priority": "HIGH"
}
```

**Validation:**
- `title`: required, max 150 chars
- `description`: required, max 5000 chars
- `priority`: required, one of `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

**Response (201 Created):**
```json
{
  "id": 1,
  "title": "Login page broken",
  "description": "Unable to access login page since morning.",
  "status": "OPEN",
  "priority": "HIGH",
  "createdById": 1,
  "assignedToId": null,
  "createdAt": "2026-04-29T10:30:00",
  "updatedAt": "2026-04-29T10:30:00"
}
```

**Notes:** New tickets are always created with status `OPEN` and no assignee.

---

#### GET `/api/tickets/me?page=0&size=10`

Get the authenticated user's tickets (paginated, newest first).

**Headers:** `Authorization: Bearer <token>`

**Query Params:**
| Param | Default | Description |
|---|---|---|
| `page` | 0 | Zero-based page index |
| `size` | 10 | Page size |

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Login page broken",
      "description": "...",
      "status": "OPEN",
      "priority": "HIGH",
      "createdById": 1,
      "assignedToId": null,
      "createdAt": "2026-04-29T10:30:00",
      "updatedAt": "2026-04-29T10:30:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### Actuator

| Endpoint | Access |
|---|---|
| `GET /actuator/health` | Public |
| `GET /actuator/info` | Authenticated |
| `GET /actuator/metrics` | Authenticated |

---

## Error Handling

All errors return a consistent `ApiErrorResponse` structure:

```json
{
  "timestamp": "2026-04-29T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email is already registered",
  "path": "/api/auth/register",
  "details": []
}
```

### Handled Exceptions

| Exception | HTTP Status | When |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Request body validation fails |
| `BadRequestException` | 400 | Business rule violation (e.g., duplicate email) |
| `BadCredentialsException` | 401 | Wrong email/password on login |

---

## Running the Application

### Prerequisites

- Java 21+
- PostgreSQL running on `localhost:5432`
- Database `slaticket` created

### Steps

```bash
# 1. Create the database
psql -U postgres -c "CREATE DATABASE slaticket;"

# 2. Run the application (Flyway will auto-create tables)
cd slaticket
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

---

## CI, Docker, and public demo (Render)

### GitHub Actions

Workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on pushes and pull requests to `main`:

- Temurin JDK 21, Maven cache
- `./mvnw -B verify` (compiles and runs `SlaticketApplicationTests` against PostgreSQL via **Testcontainers** when Docker is available, e.g. on GitHub-hosted runners)
- On a machine **without Docker**, the smoke test is **skipped** (`@Testcontainers(disabledWithoutDocker = true)`), so `mvn verify` still succeeds locally

Optional manual deploy: [`.github/workflows/deploy-render.yml`](.github/workflows/deploy-render.yml) POSTs to a Render **Deploy Hook** if you set repository secret `RENDER_DEPLOY_HOOK_URL`.

### Production profile (`prod`)

[`application-prod.yaml`](src/main/resources/application-prod.yaml) expects:

| Variable | Purpose |
|----------|---------|
| `SPRING_DATASOURCE_URL` | JDBC URL, e.g. `jdbc:postgresql://HOST:5432/DBNAME` |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `APP_JWT_SECRET` | **Base64** string that decodes to at least 32 bytes (same format as local `app.jwt.secret` in `application.yaml`) |
| `APP_JWT_EXPIRATION_MS` | Optional; defaults to `3600000` |
| `PORT` | Set automatically on Render; mapped to `server.port` |

Generate a safe JWT secret (example):

```bash
openssl rand -base64 48
```

Paste the output as `APP_JWT_SECRET` in Render.

### Docker image

[`Dockerfile`](Dockerfile) builds a runnable JAR with `./mvnw package -DskipTests` (tests are expected to run in CI first), sets `SPRING_PROFILES_ACTIVE=prod`, and listens on `PORT`.

### Deploy on Render (resume-friendly URL)

1. Push this repository to GitHub.
2. In [Render](https://render.com), create a **PostgreSQL** database (free tier is fine for a demo).
3. Create a **Web Service** → connect the repo → **Docker** runtime, root directory same as `Dockerfile` (repo root if `Dockerfile` is at the root of the Git repo).
4. Set environment variables on the web service (from the Postgres **Internal** connection values on Render):

   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<internal-host>:5432/<database>`
   - `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` from the database panel  
   - `APP_JWT_SECRET` = output of `openssl rand -base64 48`  
   - `SPRING_PROFILES_ACTIVE` = `prod` (also set in `Dockerfile`; overriding in the dashboard is fine)

5. **Health check** path: `/actuator/health` (already public in security config).
6. After deploy, your Swagger UI link for a resume:

   `https://<service-name>.onrender.com/swagger-ui.html`

   (Cold starts on the free tier can take ~30–60 seconds.)

Optional: add a **Deploy Hook** in Render and store it as `RENDER_DEPLOY_HOOK_URL`, then run the **Deploy (Render hook)** workflow after each release.

Blueprint reference: [`render.yaml`](render.yaml) (you still need to enter DB credentials and `APP_JWT_SECRET` in the Render dashboard when `sync: false`).

**Note:** Ticket attachments are stored on the container filesystem (`uploads/tickets`); files can be lost on redeploy. Suitable for API demos, not durable file storage.

---

## What's Implemented vs. Placeholder

| Feature | Status |
|---|---|
| User registration & login (JWT) | ✅ Implemented |
| JWT authentication filter | ✅ Implemented |
| Role-based user model | ✅ Implemented (roles defined, not yet enforced per endpoint) |
| Create ticket | ✅ Implemented |
| View own tickets (paginated) | ✅ Implemented |
| Get current user profile | ✅ Implemented |
| Ticket assignment | ❌ Schema ready, no API |
| Ticket status transitions | ❌ Entity ready, no API |
| SLA deadline tracking | ❌ Not yet implemented |
| Admin/Manager/Agent specific endpoints | ❌ Not yet implemented |
| `BaseEntity` / `SecurityUtils` / `UserService` | ❌ Placeholder classes |
| `ResourceNotFoundException` / `UnauthorizedException` | ❌ Placeholder (no logic) |

