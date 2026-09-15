# Medical Card (Pulse) — Technical Architecture

Portfolio backend for a personal health record (PHR) service. A patient owns and
edits their own medical card; an assigned doctor (linked via `CareLink`) can view
and add clinical data to it. Built with Java 17, Spring Boot 3.2, MySQL 8, JWT
auth, Liquibase, MapStruct, and a small custom AOP audit layer.

This document is meant as a source for hand-redrawing diagrams in draw.io —
boxes and arrows are kept simple on purpose.

---

## 1. Tech stack

| Layer            | Technology                                      |
|-------------------|--------------------------------------------------|
| Language / runtime | Java 17, Spring Boot 3.2.4                       |
| Web                | Spring MVC (REST), springdoc-openapi (Swagger)   |
| Security           | Spring Security, JWT (jjwt), method-level `@PreAuthorize`, per-IP rate limiting on auth |
| Persistence        | Spring Data JPA / Hibernate, MySQL 8, indexed hot-path queries |
| Migrations         | Liquibase (YAML changesets)                      |
| Mapping            | MapStruct                                        |
| Cross-cutting      | Spring AOP (`AuditAspect`), `@Scheduled` (reminders) |
| Documents          | S3-compatible object storage (MinIO, AWS SDK v2) + `multipart/form-data` |
| PDF export         | Thymeleaf (HTML template) + openhtmltopdf         |
| Observability      | Spring Boot Actuator (`/actuator/health`, `/metrics`) |
| Tests              | JUnit 5, Mockito, Testcontainers (MySQL + MinIO), MockMvc |
| Packaging          | Docker, Docker Compose, container healthchecks    |

---

## 2. Package layout

```
com.vitaliy.medcard
 ├─ configuration    Security, Swagger beans
 ├─ controller        REST endpoints (one class per resource)
 ├─ dto                Request/response records + classes
 ├─ exception          Custom RuntimeExceptions
 ├─ handler            GlobalExceptionHandler (@RestControllerAdvice)
 ├─ mapper             MapStruct interfaces
 ├─ model              JPA entities
 │   └─ status         Enums (UserRole, DocumentType, ...)
 ├─ repository         Spring Data JPA repositories
 │   └─ specification  CareLinkSpecifications (dynamic search)
 ├─ aspect             @Audited annotation + AuditAspect + AuditEntryWriter
 ├─ scheduler          ReminderScanScheduler (@Scheduled)
 ├─ security            JwtUtil, JwtAuthenticationFilter, CurrentUserService
 ├─ service            Interfaces
 │   └─ impl           Implementations
 ├─ util               AgeCalculator (shared, stateless helpers)
 └─ validation         PasswordMatch, PatientAccessValidator
```

---

## 3. How everything fits together

![System diagram](medical-card-diagram.png)

---

## 4. Entity-relationship diagram

```
 ┌───────────┐  1        0..1  ┌──────────────────┐
 │   User      │────────────────│  PatientProfile     │
 │  (role:     │                │  (dateOfBirth,        │
 │  PATIENT/   │                │   bloodGroup,           │
 │  DOCTOR/    │                │   emergencyContact...)  │
 │  ADMIN)     │                └─────────┬──────────────┘
 └─────┬───────┘                          │
       │                                   │ 1
       │ doctor  1..*                      │
       │                                   ├────────────── 0..* ──▶ ┌────────────┐
       │                                   │                          │  Allergy      │
       │                     ┌─────────────┤                          └────────────┘
       │                     │             │
       │                     │             ├────────────── 0..* ──▶ ┌────────────┐
       │                     │             │                          │ Condition     │
       │                     │             │                          └────────────┘
       ▼                     │             │
 ┌───────────┐   0..*        │             ├────────────── 0..* ──▶ ┌────────────┐
 │  CareLink   │◀─────────────┘             │                          │  Reminder     │
 │ (doctor +   │        patient             │                          └────────────┘
 │  patient,   │                            │
 │  unique      │                           ├────────────── 0..* ──▶ ┌────────────┐
 │  pair)        │                          │                          │  Visit         │
 └───────────┘                              │                          │ (+ doctor FK)  │
                                             │                          └────────────┘
                                             │
                                             ├────────────── 0..* ──▶ ┌────────────┐
                                             │                          │  Document      │
                                             │                          │ (+ uploadedBy  │
                                             │                          │   FK)           │
                                             │                          └────────────┘
                                             │
                                             └────────────── 0..* ──▶ ┌────────────┐
                                                                        │  ShareLink     │
                                                                        │ (+ createdBy   │
                                                                        │   FK, token,    │
                                                                        │   expiresAt)     │
                                                                        └────────────┘

 ┌────────────────┐
 │  AuditEntry       │   standalone table — no FK, just performedByEmail (string)
 │  (action,          │   so a row always survives even if the user is later deleted
 │   methodName,        │
 │   targetId,           │
 │   outcome, timestamp) │
 └────────────────┘
```

---

## 5. API surface (by controller)

| Controller                     | Base path                          | Notes |
|---------------------------------|--------------------------------------|-------|
| `AuthenticationController`      | `/api/auth`                          | public |
| `PatientProfileController`      | `/api/patients`                      | `/me` = patient, `/{id}` = doctor/admin |
| `CareLinkController`            | `/api/care-links`                    | includes `/my-patients` search+paging |
| `AllergyController`             | `/api/patients/me\|{id}/allergies`   | |
| `ConditionController`           | `/api/patients/me\|{id}/conditions`  | |
| `VisitController`               | `/api/patients/me\|{id}/visits`      | append-only |
| `DocumentController`            | `/api/patients/me\|{id}/documents`   | multipart upload, download |
| `ReminderController`            | `/api/patients/me/reminders`         | patient-only |
| `PatientCardExportController`   | `/api/patients/me\|{id}/export`      | PDF |
| `ShareLinkController`           | `/api/patients/{id}/share-links`,<br>`/api/share-links/{token}/export` | second path is PUBLIC |
| `AuditEntryController`          | `/api/audit-entries`                 | ADMIN only |

Full request/response contracts are in Swagger UI at `/swagger-ui.html`.
