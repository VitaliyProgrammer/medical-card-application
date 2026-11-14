# 🩺 Medical Card (Taborovets) - Spring Boot Back-End

![CI](https://github.com/VitaliyProgrammer/medical-card-application/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-32CD32)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-FF69B4)
![MySQL](https://img.shields.io/badge/MySQL-8-FFD700)
![Hibernate](https://img.shields.io/badge/Hibernate-6.4-4B0082)
![Liquibase](https://img.shields.io/badge/Liquibase-Migrations-blue)
![Architecture](https://img.shields.io/badge/Architecture-Layered-lightgreen)
![DTO](https://img.shields.io/badge/DTO-MapStruct-yellow)
![Specification](https://img.shields.io/badge/Specification-Pattern-lightgrey)
![AOP](https://img.shields.io/badge/AOP-Audit%20Aspect-orange)
![Object Storage](https://img.shields.io/badge/Object%20Storage-MinIO%20%2F%20S3-red)
![Actuator](https://img.shields.io/badge/Spring%20Boot-Actuator-teal)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![Testing](https://img.shields.io/badge/Testing-JUnit%205%20%7C%20Mockito%20%7C%20Testcontainers-blueviolet)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI-85EA2D)

## 📌 Introduction

**Medical Card** (brand name **Taborovets**) is a personal health record (PHR)
back-end developed as part of my Java back-end engineering learning journey.
It replicates the core functionality of a real clinic system and demonstrates
how a production-grade service handles:

- patient self-service medical records
- doctor-side access, scoped strictly to assigned patients
- clinical data entry (visits, allergies, chronic conditions)
- document storage, PDF export, and controlled external sharing

The goal of this project is not simply to build CRUD endpoints, but to design
a **clean, scalable, and secure architecture** that follows production-grade
principles such as:

- layered architecture
- DTO mapping (MapStruct)
- custom exception handling
- Specification pattern for dynamic search
- ownership-based authorization (not just role checks)
- a custom AOP audit aspect
- JWT-based authentication with per-IP rate limiting
- S3-compatible object storage
- Liquibase migrations with indexed hot-path queries
- mocking and integration testing (Testcontainers)

## 🎯 Motivation

This project was built as a deliberate follow-up to an earlier, more complex
project of mine (an e-commerce back-end) — same architectural discipline,
applied to a domain with a very different central problem: not "who owns this
row" under concurrent writes, but **who is allowed to see this patient at
all**.

While working on Medical Card, I focused on solving problems specific to
healthcare-shaped data:

- 🔐 How do you model "a doctor can see exactly the patients assigned to
  them" as a reusable rule, not a copy-pasted check in every service?
- 🧾 How do you keep an audit trail that survives a failed transaction,
  without reaching for Hibernate Envers "magic" you can't fully explain in an
  interview?
- 📄 How do you generate a PDF export once and reuse the exact same pipeline
  for a temporary, tokenized public link — without duplicating the rendering
  logic?
- ⏰ How do you run a periodic reminder scan with no message broker, no
  backpressure, just one instance and one scan?
- 🗄️ How do you move file storage from local disk to S3-compatible object
  storage (MinIO) without changing the service's public contract?
- 🧪 How do you write integration tests that exercise real MySQL *and* real
  object storage, both disposable, both spun up automatically?

Working through these questions sharpened my understanding of authorization
design, Spring AOP, transaction propagation, and infrastructure-as-code
testing.

## 🚀 What this project demonstrates

- 🧠 Clean, layered back-end architecture
- 🔒 Real authorization model — role-based access **and** per-record
  ownership, not just `@PreAuthorize("hasRole(...)")`
- 🛡️ Defensive security practices: rate-limited auth endpoints, no PII in
  logs, indexed queries, graceful shutdown
- 📐 Patterns carried over deliberately from an earlier project and re-applied
  to a new domain (DTO+MapStruct, Specification search, JWT auth, Liquibase,
  centralized exception handling)
- 🏥 A believable clinical workflow end to end

## 📌 Features / Functionality

👤 **Authentication & authorization**
- Register as `PATIENT` or `DOCTOR`, JWT login
- Role-based access control combined with per-record ownership (`CareLink`)
- Rate-limited login/registration (`429` after 5 attempts/minute per IP)

🩺 **Patient medical card**
- Profile with computed age, blood group, emergency contact
- Allergies and chronic conditions, managed by the patient
- Append-only visit history, authored by the assigned doctor

📎 **Documents**
- `multipart/form-data` upload to S3-compatible object storage (MinIO)
- Type/size validation, ownership-checked download, filter by document type

🔊 **Audit log**
- Custom AOP aspect (`@Around`) wraps every state-changing service method
- Writes in its own `REQUIRES_NEW` transaction, so a `FAILURE` is recorded
  even when the underlying business transaction rolls back

⏰ **Reminders**
- Patient-created medication/appointment reminders
- `@Scheduled` cron job flags overdue ones every 5 minutes

📄 **PDF export & temporary share link**
- Thymeleaf template rendered to PDF via openhtmltopdf
- A doctor can generate a 48-hour public, tokenized link that reuses the
  exact same export pipeline — no separate rendering code path
- The token is stored only as a SHA-256 hash, every public download is
  audited, and the doctor can revoke the link before it expires

🔍 **Filtering and searching**
- Powered by the **Specification** pattern for a doctor's paginated,
  searchable patient list

✅ **Validation**
- Spring Validation annotations on every DTO, plus a custom `@PasswordMatch`
  validator

🔐 **Security**
- Short-lived JWT access tokens (15 min) with a separate, DB-backed,
  single-use refresh token (rotated on every use, revocable on logout)
- Ownership checks on every patient record, per-IP rate limiting, indexed
  hot-path queries

🧪 **Testing**
- Mockito-based unit tests for every service
- Integration tests against real, disposable MySQL and MinIO containers
  (Testcontainers)

Overall, this section demonstrates a realistic clinical workflow:
**`register → get linked to a doctor → record a visit → export or share the card`**

## 🏗 Architecture & Technology Stack

This project follows a **layered architecture** to separate concerns and keep
it maintainable:

- **Presentation Layer / Controller** — handles HTTP requests, implements the
  REST endpoints
- **Service Layer** — business rules, ownership checks, transaction
  boundaries
- **Repository Layer** — Spring Data JPA against MySQL, with indexes on every
  hot-path query
- **DTO Layer** — MapStruct-mapped request/response contracts, decoupled from
  entities
- **AOP Layer** — a single `@Around` aspect drives the entire audit log
- **Security Layer** — JWT authentication, method-level authorization, per-IP
  rate limiting
- **Exception Handling** — one centralized `@RestControllerAdvice` with a
  handler per custom exception
- **Testing Layer** — Mockito for services, Testcontainers (MySQL + MinIO + Redis)
  for repositories/controllers

## Technology Stack

| Technology / Tool             | Version         | Purpose                                                        |
|--------------------------------|------------------|------------------------------------------------------------------|
| Java                            | 17               | Core programming language                                        |
| Spring Boot                     | 3.2.4            | Application framework                                             |
| Spring Security + JWT           | Boot-managed + jjwt 0.11.5 | Authentication & authorization                       |
| MySQL                           | 8                | Database                                                          |
| Hibernate                       | 6.4 (Boot-managed) | ORM; entity-to-table mapping, schema validation                |
| Liquibase                       | Boot-managed     | Database migration & versioning, with performance indexes         |
| MapStruct                       | 1.5.5.Final      | DTO ↔ entity mapping                                              |
| AWS SDK v2 (S3)                 | 2.25.60          | S3-compatible object storage client (MinIO in dev/compose)        |
| Redis (Spring Data Redis)       | 7.4              | Rate-limit counters shared across application instances           |
| openhtmltopdf                   | 1.0.10           | HTML (Thymeleaf) → PDF rendering for card export                  |
| Spring Boot Actuator + Micrometer/Prometheus | Boot-managed | Health checks, `/actuator/prometheus` metrics |
| JUnit 5 / Mockito / Testcontainers | 5.10.2 / Boot-managed / 1.21.4 | Unit and integration testing (MySQL, MinIO, Redis) |
| Docker / Docker Compose         | -                | Containerization of app, MySQL, MinIO and Redis                   |
| GitHub Actions                  | -                | CI: build, Checkstyle, unit + Testcontainers tests on every push  |
| Swagger (springdoc-openapi)     | 2.5.0            | API documentation & testing                                        |

## Design Patterns && Architecture Concepts

| Concept / Pattern               | Purpose                                                     |
|-----------------------------------|-----------------------------------------------------------|
| Layered Architecture              | Maintainable, scalable system                              |
| Repository Pattern                | Clean separation of data access logic                      |
| Specification Pattern             | Dynamic, paginated search of a doctor's patients            |
| AOP Audit Aspect                  | Cross-cutting "who changed what, when" without touching business code |
| Ownership Validator               | One reusable rule for "is this doctor allowed to see this patient" |
| DTO + MapStruct                   | Clean API contracts, decoupled from JPA entities            |
| Centralized Exception Handling    | Predictable, consistent error responses                     |
| Rate Limiting Filter              | Redis-backed fixed-window counter on auth endpoints, shared by all instances |
| Refresh-Token Rotation            | Short-lived JWT + DB-backed, single-use, revocable refresh token |
| Request Correlation Filter        | Per-request ID (MDC) threaded through all log lines for traceability |

<div align="center">

## System Diagram

![System diagram](docs/medical-card-diagram.png)

</div>

Entity-relationship diagram and package layout:
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## 🐳 Infrastructure & Deployment

The application is fully containerized using Docker and Docker Compose, so
anyone can run the full stack without installing Java, Maven, or MySQL
locally.

- **Docker** — a multi-stage build compiles the application from source with
  the Maven wrapper, then runs it on a slim Java runtime as a non-root user,
  with `curl` for its own container healthcheck against `/actuator/health`
- **Docker Compose** — orchestrates four containers:
  - Spring Boot application
  - MySQL database (with a healthcheck gate before the app starts)
  - MinIO (S3-compatible object storage, with its own healthcheck gate)
  - Redis (rate-limit counters shared across app instances, healthcheck gated)

This mirrors real-world deployment practice: the app only starts once its
dependencies report healthy, and the app itself only reports healthy once its
own `/actuator/health` responds.

## 🛠 Local Setup / Getting Started

The application is fully containerized and can be started locally without
manual database setup, Liquibase configuration, or environment tuning.

**Quick start** — four commands, nothing else to install or edit:

```bash
git clone https://github.com/VitaliyProgrammer/medical-card-application.git
cd medical-card-application
cp .env.template .env
docker compose up --build -d --wait
```

When the last command returns, the API is ready at
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
The steps below explain each command.

### 1. Prerequisites

- **Docker** with **Docker Compose v2** (Docker Desktop on Windows/macOS
  includes both) — Java, Maven and MySQL are **not** needed on the host
- Free ports **8080** (API) and **9001** (MinIO console)

```bash
docker --version
docker compose version
```

### 2. Clone the repository

```bash
git clone https://github.com/VitaliyProgrammer/medical-card-application.git
cd medical-card-application
```

### 3. Configure environment

```bash
cp .env.template .env
```

The template already holds throwaway local values, so nothing needs editing
for a review run. Change them (especially `JWT_SECRET`) for any real deployment.
If this step is skipped, `docker compose` stops straight away with
`Run cp .env.template .env first`.

> `JWT_SECRET` is required and must be at least 32 characters (256 bits).
> The application itself has deliberately no default: it refuses to start
> without it rather than silently signing tokens with a well-known key.

### 4. Run the application

```bash
docker compose up --build -d --wait
```

This command will:
- **build the Spring Boot application image from source** inside Docker
  (multi-stage build, so no local Java or Maven is needed)
- **start MySQL, MinIO and Redis** and wait until each reports healthy
- **apply Liquibase migrations** and create the documents bucket on first boot
- **return only once the application itself is healthy**

> ⏱️ The first run downloads Maven dependencies and creates the schema, so it
> can take 5–20 minutes depending on the machine and network. Later runs
> start much faster. To watch progress: `docker compose logs -f app`.

Check that everything is up:

```bash
docker compose ps                              # all four services "healthy"
curl http://localhost:8080/actuator/health     # {"status":"UP"}
```

### 5. Stop the application

```bash
docker compose down          # stop containers, keep the data volumes
docker compose down -v       # stop and wipe all data volumes too
```

### 6. Try it out

The easiest way is **Swagger UI** at
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html):
register, log in, copy the `token` from the response, press **Authorize** and
paste it — every endpoint can then be called from the browser.

The same flow with `curl` (bash syntax — on Windows use Git Bash or WSL, or
just use Swagger UI):

```bash
# Register a patient
curl -X POST http://localhost:8080/api/auth/registration \
  -H "Content-Type: application/json" \
  -d '{
        "email": "patient@example.com",
        "password": "Password123!",
        "repeatPassword": "Password123!",
        "fullName": "Jane Patient",
        "role": "PATIENT"
      }'

# Log in
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "patient@example.com", "password": "Password123!"}'
# → { "token": "...", "refreshToken": "..." }

# Read your own card
curl http://localhost:8080/api/patients/me \
  -H "Authorization: Bearer <token>"

# When the access token expires (after 15 min), exchange the refresh token
# for a new pair instead of logging in again — the old refresh token is
# revoked in the same call, so it can't be reused
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<refreshToken>"}'
```

Register a second account with `"role": "DOCTOR"`, then, as the patient, grant
that doctor access with `POST /api/patients/me/care-links {"doctorEmail": "..."}`
(access can be revoked any time via `DELETE /api/patients/me/care-links/{id}`).
Only then can the doctor use the doctor-side
endpoints (`/api/care-links/my-patients`, viewing the patient's card, recording a
visit, etc.) — see Swagger UI for the full list.

> All dates in requests and responses are ISO-8601: `yyyy-MM-dd` for dates,
> `yyyy-MM-ddTHH:mm:ss` for date-times; error timestamps are UTC instants.

> Login and registration are rate-limited (5 requests/minute per IP) — you'll
> get `429 Too Many Requests` if you hit either endpoint faster than that.
> The counters live in Redis, so the limit holds across several app instances.
> Behind a reverse proxy set `FORWARD_HEADERS_STRATEGY=native` so the real
> client IP is used; leave it at `none` when the app is exposed directly,
> otherwise a client could forge `X-Forwarded-For` to dodge the limit.

> `ADMIN` accounts can't self-register on purpose. To try the
> `/api/audit-entries` or `/actuator/metrics` endpoints, promote a registered
> user directly in the database (the password is the one from `.env.template`),
> then **log in again** — the role is stored inside the JWT:
>
> ```bash
> docker compose exec mysqldb mysql -uroot -proot_pass medical_card_application \
>   -e "UPDATE users SET role = 'ADMIN' WHERE email = 'patient@example.com';"
> ```

> MinIO console ([http://localhost:9001](http://localhost:9001)) login:
> `medcard_storage` / `medcard_storage_pass` (from `.env.template`).

## Running the tests

```bash
./mvnw test
```

Unit tests (Mockito) run with no external dependencies. Repository/controller
integration tests use [Testcontainers](https://testcontainers.com/) to spin up
real, disposable MySQL, MinIO and Redis containers — Docker must be running
for those. Unlike the Docker quick start, running the tests needs JDK 17 on
the host (the Maven wrapper downloads Maven itself).

## 📘 API Documentation

All endpoints are documented in Swagger UI:
[Open Swagger UI](http://localhost:8080/swagger-ui.html)

Health check: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
Metrics (ADMIN only): [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)
MinIO console: [http://localhost:9001](http://localhost:9001)

## 📌 Final Notes

This project reflects my approach to back-end engineering: thoughtful
architecture, clear separation of concerns, security-first design, and
realistic workflows — deliberately scoped a notch below my previous project's
complexity, so every design decision here (AOP over Envers, a hand-written
rate limiter over a library, one shared PDF pipeline for two entry points)
is one I can defend in detail. It's both a learning milestone and a
foundation for future production-ready systems.
