# Medical Card (Pulse) — Technical Architecture

Portfolio backend for a personal health record (PHR) service. A patient owns and
edits their own medical card; an assigned doctor (linked via `CareLink`) can view
and add clinical data to it. Built with Java 17, Spring Boot 3.2, MySQL 8, JWT
auth, Liquibase, MapStruct, and a small custom AOP audit layer.

The request-flow diagram below was hand-drawn in draw.io; the
entity-relationship diagram is rendered directly from Mermaid source, so it
always stays aligned.

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

```mermaid
erDiagram
    USER ||--o| PATIENT_PROFILE : "has"
    USER ||--o{ CARE_LINK : "doctor in"
    PATIENT_PROFILE ||--o{ CARE_LINK : "patient in"
    PATIENT_PROFILE ||--o{ ALLERGY : "has"
    PATIENT_PROFILE ||--o{ CONDITION : "has"
    PATIENT_PROFILE ||--o{ VISIT : "has"
    PATIENT_PROFILE ||--o{ DOCUMENT : "has"
    PATIENT_PROFILE ||--o{ REMINDER : "has"
    PATIENT_PROFILE ||--o{ SHARE_LINK : "has"
    USER ||--o{ VISIT : "recorded by (doctor)"
    USER ||--o{ DOCUMENT : "uploaded by"
    USER ||--o{ SHARE_LINK : "issued by"

    USER {
        Long id PK
        string email
        UserRole role
    }
    PATIENT_PROFILE {
        Long id PK
        Long user_id FK
        date dateOfBirth
        string bloodGroup
    }
    CARE_LINK {
        Long id PK
        Long doctor_id FK
        Long patient_id FK
    }
    ALLERGY {
        Long id PK
        Long patient_id FK
        string name
        AllergySeverity severity
    }
    CONDITION {
        Long id PK
        Long patient_id FK
        string name
        ConditionStatus status
    }
    VISIT {
        Long id PK
        Long patient_id FK
        Long doctor_id FK
        datetime visitDate
        string diagnosis
    }
    DOCUMENT {
        Long id PK
        Long patient_id FK
        Long uploaded_by FK
        DocumentType documentType
        string storedFileName
    }
    REMINDER {
        Long id PK
        Long patient_id FK
        ReminderType type
        datetime dueAt
        boolean notified
    }
    SHARE_LINK {
        Long id PK
        Long patient_id FK
        Long created_by FK
        string token
        datetime expiresAt
    }
    AUDIT_ENTRY {
        Long id PK
        string performedByEmail
        string action
        string outcome
        datetime timestamp
    }
```

`AUDIT_ENTRY` is drawn with no relationship lines on purpose — it has no FK at
all, just a `performedByEmail` string, so a row survives even after the user
who triggered it is deleted.

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
