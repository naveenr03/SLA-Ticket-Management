# SLA Ticket

REST API for support ticket management: JWT authentication, role-based rules, ticket lifecycle (create, assign, status), comments, audit history, and file attachments. Built with **Spring Boot 4** and **PostgreSQL**.

---

## Features

- **Authentication** — Register and login; stateless **JWT** (Bearer) for protected routes.
- **Tickets** — Create tickets with priority; list tickets you created or that are assigned to you (paginated).
- **Assignment** — Managers and admins can assign tickets to agents.
- **Status** — Update ticket status with validation based on who you are (creator, assignee, role).
- **Collaboration** — Comments and read-only **history** for auditing.
- **Attachments** — Upload, list, and download files (stored on local disk by default; not durable across container redeploys without external storage).
- **Observability** — Spring Boot Actuator (`health`, `info`, `metrics`).
- **API docs** — OpenAPI 3 and Swagger UI (no auth required for the UI).

For request/response examples, schema notes, and deployment detail, see **[DOCUMENTATION.md](DOCUMENTATION.md)**.

---

## Tech stack

| Layer | Technology |
|--------|------------|
| Runtime | Java **21** |
| Framework | Spring Boot **4.0.x** (Web, Security, Data JPA, Validation, Actuator) |
| Database | **PostgreSQL** |
| Migrations | **Flyway** |
| API docs | **springdoc-openapi** (Swagger UI) |
| Security | **JJWT**, BCrypt |
| Build | **Maven** (wrapper included) |
| Tests | JUnit 5, **Testcontainers** (PostgreSQL; skipped when Docker is unavailable) |

---

## Prerequisites

- **JDK 21** (e.g. Eclipse Temurin)
- **PostgreSQL** reachable from the app (default local URL in config)
- **Docker** (optional) — for `docker build` / full `mvn verify` with Testcontainers

---

## Quick start

### 1. Create the database

```bash
psql -U postgres -c "CREATE DATABASE slaticket;"
```

Adjust host, user, and password to match your environment.

### 2. Configure local defaults (optional)

Default connection and JWT settings live in [`src/main/resources/application.yaml`](src/main/resources/application.yaml). Override any value with environment variables or an additional Spring profile if you prefer not to edit files.

> **Security:** Do not use the committed development JWT secret in production. Use a strong random Base64 secret (see [DOCUMENTATION.md](DOCUMENTATION.md#production-profile-prod)).

### 3. Run the application

From the repository root:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway runs migrations on startup (including optional seed users for agent/manager/admin roles — see `src/main/resources/db/migration/V3__seed_support_users.sql`).

The API listens on **http://localhost:8080** by default.

### 4. Explore the API

| Resource | URL |
|----------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health (public) | http://localhost:8080/actuator/health |

In Swagger UI, use **Authorize** and set `Bearer <accessToken>` after registering or logging in via `/api/auth/register` or `/api/auth/login`.

---

## Common commands

| Goal | Command |
|------|---------|
| Run app | `./mvnw spring-boot:run` |
| Run tests | `./mvnw verify` |
| Build JAR | `./mvnw package` |
| Docker image | `docker build -t slaticket:local .` |

---

## Production / cloud

- Activate the **`prod`** profile (`SPRING_PROFILES_ACTIVE=prod`).
- Set **`SPRING_DATASOURCE_URL`**, **`SPRING_DATASOURCE_USERNAME`**, **`SPRING_DATASOURCE_PASSWORD`**, and **`APP_JWT_SECRET`** (Base64, decodes to a sufficiently long key for HS256). Optional: **`APP_JWT_EXPIRATION_MS`**, **`PORT`** (Render and similar set `PORT` automatically).

The [`Dockerfile`](Dockerfile) builds a JAR with the Maven wrapper, runs as a non-root user, and defaults to the `prod` profile. See **[DOCUMENTATION.md](DOCUMENTATION.md#ci-docker-and-public-demo-render)** for Render, GitHub Actions, and `render.yaml` notes.

---

## Project layout

```
src/main/java/com/naveen/slaticket/
├── SlaticketApplication.java    # Entry point
├── auth/                        # Register, login, JWT, security config
├── user/                        # User profile API
├── ticket/                      # Tickets, comments, history, attachments
├── common/                      # Errors, shared DTOs
└── config/                      # OpenAPI, JPA auditing

src/main/resources/
├── application.yaml             # Default (local) settings
├── application-prod.yaml        # Production placeholders / env bindings
└── db/migration/                # Flyway SQL migrations
```

---

## CI

Pushes and pull requests to **`main`** run [`.github/workflows/ci.yml`](.github/workflows/ci.yml): `./mvnw -B verify` and a sanity **`docker build`**.

---

## Documentation

- **[DOCUMENTATION.md](DOCUMENTATION.md)** — Extended API reference, configuration tables, database schema, error format, and deployment checklist.

If anything in the long-form doc disagrees with the live controllers, treat **Swagger UI** and the Java sources as the source of truth.

---
