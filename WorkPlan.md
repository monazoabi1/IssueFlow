# IssueFlow — Work Plan

**Last updated:** 2026-05-23  
**Status:** Homework scope implemented — REST APIs, extended features, and automated tests. Full suite: **152 tests**, **0 failures** (`.\mvnw.cmd test`).

| Artifact | Role |
|----------|------|
| `README.md` | API contract (paths, bodies, status codes) |
| `run.md` | Install, database, build, run app, run tests |
| `prompts.md` | AI prompts used during development |
| `WorkPlan.md` | This file — architecture, progress, test map |

---

## Summary of work completed

IssueFlow is a Spring Boot 3.4 / Java 21 ticket-management backend backed by **PostgreSQL** at runtime and **H2** in tests. All domains from `README.md` are implemented:

| Area | Delivered |
|------|-----------|
| Core CRUD | Users, projects, tickets, nested comments |
| Auth | JWT login, logout (token blacklist), `/auth/me` |
| Audit | Append-only log on create/update/delete; filtered `GET /audit-logs` |
| Soft delete | Tickets & projects; list deleted + restore (**ADMIN** only) |
| Mentions | `@username` parsing in comments; paginated `GET /users/{id}/mentions` |
| Dependencies | Blocker links between tickets |
| Attachments | Multipart upload; 10 MB / type validation |
| CSV | `GET /tickets/export`, `POST /tickets/import` with per-row errors |
| Workload | `GET /projects/{id}/workload` (open tickets per developer) |
| Auto-assignment | Least-loaded `DEVELOPER` when `assigneeId` omitted on create |
| Auto-escalation | `@Scheduled` job raises priority when `dueDate` passed |
| Concurrency | `@Version` optimistic locking on tickets; `409` on conflict |

---

## Project layout

Base package: `com.att.tdp.issueflow`. Prefer **lowercase** package folders (`controller`, `service`). Active config: `src/main/resources/application.yaml`.

### Repository root

```text
issueflow-java/
├── compose.yml              # PostgreSQL (Docker)
├── pom.xml                  # Spring Boot 3.4.2, Java 21, JPA, Security, JWT, Commons CSV
├── README.md                # API contract
├── run.md                   # Setup and run guide
├── prompts.md               # AI prompts (fundamentals, planning, REST)
├── WorkPlan.md              # This file
└── src/
    ├── main/java/.../issueflow/
    │   ├── IssueFlowApplication.java   (@EnableScheduling)
    │   ├── controller/                 # 8 REST controllers
    │   ├── service/                    # Business logic + schedulers
    │   ├── repository/                 # Spring Data JPA
    │   ├── model/                      # @Entity classes
    │   ├── dto/                        # Request/response DTOs
    │   ├── config/                     # Security, JWT filter, properties
    │   ├── validation/                 # Attachment + CSV validators
    │   └── Exception/                  # Exceptions + GlobalExceptionHandler
    ├── main/resources/application.yaml
    └── test/
        ├── java/.../issueflow/         # 35 test classes (see below)
        └── resources/application.yaml  # H2 in-memory
```

### Request flow

```text
Client  →  controller  →  service  →  repository  →  PostgreSQL
                              ↓
                    audit log (CREATE / UPDATE / DELETE / AUTO_ASSIGN)
                              ↓
              schedulers (escalation, token blacklist cleanup)
```

### Layer responsibilities

| Package | Responsibility | Key classes |
|---------|----------------|-------------|
| `controller` | HTTP mapping, `@Valid`, status codes | `UserController`, `TicketController`, `AuthController`, … |
| `service` | Rules, transactions, audit, CSV, mentions | `TicketService`, `CommentService`, `AuditLogService`, `JwtService` |
| `repository` | Persistence queries | `TicketRepository`, `CommentMentionRepository`, … |
| `model` | JPA entities + nested enums | `UserEntity`, `TicketEntity`, `AuditLogEntity` |
| `dto` | JSON contracts (no passwords in responses) | `CreateUserRequest`, `TicketResponse`, `LoginResponse` |
| `config` | Security filter chain, JWT properties | `SecurityConfig`, `JwtAuthenticationFilter` |
| `validation` | File/CSV rules before persist | `AttachmentContentValidator`, `TicketCsvValidator` |
| `Exception` | Typed errors → HTTP status | `GlobalExceptionHandler`, `ConflictException`, `ForbiddenException` |

---

## Implementation phases (actual delivery)

| Phase | Scope | Status |
|-------|--------|--------|
| 1 | Users CRUD, BCrypt passwords, DTOs, `GlobalExceptionHandler` | Done |
| 2 | Projects CRUD, owner `@ManyToOne` | Done |
| 3 | Tickets CRUD, status rules, soft delete, optimistic locking | Done |
| 4 | Nested comments under `/tickets/{id}/comments` | Done |
| 5 | JWT auth (`/auth/login`, `/logout`, `/me`), `JwtAuthenticationFilter` | Done |
| 6 | Audit log (automatic writes + `GET /audit-logs`) | Done |
| 7 | Mentions, paginated mentions API | Done |
| 8 | Ticket dependencies, attachments, CSV export/import | Done |
| 9 | Soft-delete list/restore (ADMIN), project workload | Done |
| 10 | Auto-assignment, auto-escalation scheduler | Done |
| 11 | Unit, integration, controller, and stress tests | Done |

---

## Feature completion (by API domain)

### Users & authentication

| Piece | Status | Notes |
|-------|--------|--------|
| `UserEntity` / `UserRepository` | Done | Unique username; `findByUsername`, `existsByUsername` |
| `UserService` / `UserController` | Done | `GET/POST /users`, `GET/PUT/DELETE /users/{id}`; BCrypt on password |
| `AuthService` / `AuthController` | Done | `POST /auth/login`, `POST /auth/logout`, `GET /auth/me` |
| `JwtService` | Done | HS256 tokens; `issueflow.jwt.*` in `application.yaml` |
| `TokenBlacklistService` | Done | Logout invalidates JWT; scheduled cleanup |
| `SecurityConfig` | Done | Stateless; login public; `/auth/me` and `/auth/logout` require Bearer token |
| Mentions | Done | `GET /users/{userId}/mentions?page=&pageSize=` |

**REST note:** README lists `POST /users/update/:id`; implementation uses **`PUT /users/{id}`** (more idiomatic).

### Projects

| Piece | Status | Notes |
|-------|--------|--------|
| `ProjectEntity` / `ProjectService` / `ProjectController` | Done | CRUD, soft delete, restore |
| Soft delete (ADMIN) | Done | `GET /projects/deleted`, `POST /projects/{id}/restore` |
| Workload | Done | `GET /projects/{projectId}/workload` — open ticket counts per developer |

### Tickets

| Piece | Status | Notes |
|-------|--------|--------|
| `TicketEntity` (`ticketId` PK) / `TicketRepository` | Done | `findByTicketIdAndIsDeletedFalse`, escalation query |
| `TicketService` / `TicketController` | Done | List by `projectId`, CRUD, soft delete, restore |
| Status transitions | Done | Enforced in service layer |
| Optimistic locking | Done | `version` on entity; required on `PATCH`; `409` on conflict |
| CSV export/import | Done | `TicketCsvService`, `TicketCsvValidator`, Apache Commons CSV |
| Auto-assignment | Done | `TicketAssignmentService` on create when no assignee |
| Auto-escalation | Done | `TicketEscalationService` — `@Scheduled`, configurable interval |

### Comments

| Piece | Status | Notes |
|-------|--------|--------|
| `CommentEntity` / `CommentMentionEntity` | Done | Mentions stored in `comment_mentions` |
| `CommentService` / `CommentController` | Done | Nested under `/tickets/{ticketId}/comments` |
| `@username` parsing | Done | Regex extract; validate users exist; return in `CommentResponse` |

### Audit, dependencies, attachments

| Piece | Status | Notes |
|-------|--------|--------|
| `AuditLogService` / `AuditLogController` | Done | Logged from user/project/ticket/comment services; read-only API with filters |
| `TicketDependencyService` / `TicketDependencyController` | Done | Cycle prevention; `POST/GET/DELETE` under `/tickets/{id}/dependencies` |
| `AttachmentService` / `AttachmentController` | Done | Max 10 MB; PNG, JPEG, PDF, plain text |

---

## REST controllers (implemented endpoints)

| Controller | Base path | Main operations |
|------------|-----------|-----------------|
| `AuthController` | `/auth` | login, logout, me |
| `UserController` | `/users` | CRUD, mentions |
| `ProjectController` | `/projects` | CRUD, deleted list, restore, workload |
| `TicketController` | `/tickets` | CRUD, export, import, deleted list, restore |
| `CommentController` | `/tickets/{ticketId}/comments` | list, create, patch, delete |
| `TicketDependencyController` | `/tickets/{ticketId}/dependencies` | add, list, remove blocker |
| `AttachmentController` | `/tickets/{ticketId}/attachments` | upload, delete |
| `AuditLogController` | `/audit-logs` | filtered list (newest first) |

Security today: **only** `/auth/me` and `/auth/logout` strictly require JWT; other routes are open but accept optional Bearer tokens for `performedBy` in audit logs. ADMIN-only operations call `AuthService.requireAdmin()` (403 if not admin).

---

## Cross-cutting

| Piece | Status | Details |
|-------|--------|---------|
| `GlobalExceptionHandler` | Done | 400, 401, 403, 404, 409; optimistic lock → 409; upload size → 400 |
| Password hashing | Done | `BCryptPasswordEncoder` |
| Scheduling | Done | `@EnableScheduling` on main app; escalation + token blacklist |
| Multipart limits | Done | 10 MB in `application.yaml` |
| Audit actions | Done | `CREATE`, `UPDATE`, `DELETE`, `AUTO_ASSIGN`; actor `USER` / `SYSTEM` |

---

## Test coverage

Tests use **H2** (`src/test/resources/application.yaml`). **Docker is not required** for `mvn test`.

| Package | Test classes | Focus |
|---------|--------------|--------|
| `repository` | 5 | JPA queries, soft-delete flags, escalation candidates |
| `service` | 10 | Business rules, audit, CSV, assignment, escalation, concurrency |
| `controller` | 13 | MockMvc / full slice HTTP contracts |
| `validation` | 2 | Attachment types/size, CSV row validation |
| `stress` | 6 | Concurrent updates, mentions, soft-delete, dependencies, assignment, escalation |
| (root) | 1 | `IssueFlowApplicationTests` — context loads |

**Total:** 152 test methods across 35 classes (last verified 2026-05-23).

### Commands

See **[run.md](run.md)** for JDK 21 setup and full commands. From project root:

```powershell
# All tests
.\mvnw.cmd test

# By layer
.\mvnw.cmd test -Dtest=com.att.tdp.issueflow.controller.*Test
.\mvnw.cmd test -Dtest=com.att.tdp.issueflow.service.*Test
.\mvnw.cmd test -Dtest=com.att.tdp.issueflow.repository.*Test
.\mvnw.cmd test -Dtest=com.att.tdp.issueflow.stress.*Test

# Examples — single feature
.\mvnw.cmd test -Dtest=AuthControllerTest
.\mvnw.cmd test -Dtest=TicketServiceTest,TicketServiceConcurrentUpdateTest
.\mvnw.cmd test "-Dtest=AuditLogServiceTest,AuditLogControllerTest"
.\mvnw.cmd test "-Dtest=AttachmentContentValidatorTest,AttachmentControllerTest"
.\mvnw.cmd test "-Dtest=TicketCsvValidatorTest,TicketCsvServiceTest,TicketExportImportControllerTest"
```

Reports: `target/surefire-reports/`.

---

## Entity tables (JPA)

One `@Entity` per table in `model/`. Auth has **no** separate table — login uses `users` + JWT services.

| # | Table | Entity | Key relations / notes |
|---|--------|--------|------------------------|
| 1 | `users` | `UserEntity` | `username` unique; `Role` enum (`DEVELOPER`, `ADMIN`, …); BCrypt password |
| 2 | `projects` | `ProjectEntity` | `owner_id` → user; `isDeleted` flag |
| 3 | `tickets` | `TicketEntity` | PK `ticket_id`; `project_id`, `assignee_id`; `version`; `isDeleted`; `dueDate`, `isOverdue` |
| 4 | `comments` | `CommentEntity` | `ticket_id`, `author_id`, `content` |
| 5 | `comment_mentions` | `CommentMentionEntity` | `comment_id`, `user_id` |
| 6 | `audit_logs` | `AuditLogEntity` | Append-only; `action`, `entity_type`, `entity_id`, `performed_by`, `actor` |
| 7 | `ticket_dependencies` | `TicketDependencyEntity` | `ticket_id`, `blocked_by_ticket_id` |
| 8 | `attachments` | `AttachmentEntity` | Binary `data` in DB; filename, content type |

**Repositories:** `UserRepository`, `ProjectRepository`, `TicketRepository`, `CommentRepository`, `CommentMentionRepository`, `AuditLogRepository`, `TicketDependencyRepository`, `AttachmentRepository`.

Enums (`TicketStatus`, `TicketPriority`, `TicketType`, `UserEntity.Role`, …) live on entities as `@Enumerated(STRING)`.

---

## Fixes discovered while building

1. **`ProjectEntity.owner`** — requires `@ManyToOne` + `@JoinColumn(name = "owner_id")` or Hibernate cannot map the FK.
2. **`TicketRepository` method names** — use `ticketId` (entity field), e.g. `findByTicketIdAndIsDeletedFalse`, not `findById…`.
3. **`UserRepositoryTest`** — `@DataJpaTest` can scope to `UserRepository` only during incremental work to avoid loading broken mappings early.
4. **JDK version** — project targets **Java 21**; JDK 25 caused Lombok/compile issues in this environment.
5. **Windows `PATH`** — Oracle `javapath` can shadow JDK 21; prepend `%JAVA_HOME%\bin` (see `run.md`).

---

## Optional cleanup (not blocking)

| Item | Notes |
|------|--------|
| Duplicate `application.yaml` | Copy under `src/main/java/.../resources/` can be removed; canonical file is `src/main/resources/application.yaml` |
| Package casing | Prefer lowercase `exception` vs `Exception` for consistency with Java conventions |
| JWT on all routes | README suggests broader JWT protection; current design only enforces auth on `/auth/me` and `/auth/logout` |
| User update verb | `PUT /users/{id}` vs README `POST /users/update/{id}` — document choice for graders |

---

## Session workflow (daily dev)

Full step-by-step instructions: **[run.md](run.md)**.

```powershell
cd path\to\issueflow-java

# JDK 21 if needed (see run.md)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

docker compose -f compose.yml up -d    # before spring-boot:run
.\mvnw.cmd clean package               # build
.\mvnw.cmd spring-boot:run             # API http://localhost:8080
.\mvnw.cmd test                        # tests (no Docker)
```

---

## AI & documentation artifacts

| File | Purpose |
|------|---------|
| `prompts.md` | Representative prompts (fundamentals, planning, REST design) and which agents were used |
| `run.md` | Homework section 4.4 — exact install/build/run/test steps |
| `README.md` | Source of truth for API contract |
