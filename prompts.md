# Prompts used during the work

**Agents used:** ChatGPT (GPT-5), Claude (Anthropic), Gemini, Cursor (Composer)  
**Purpose:** Representative prompts for **fundamentals**, **planning**, and **REST API design** (not verbatim chat history).

---

## Fundamentals

| Topic | Agent | Example prompt |
|--------|--------|----------------|
| Stack overview | ChatGPT | For a Spring Boot 3 + JPA + PostgreSQL homework backend, list the Maven dependencies I need for REST APIs, validation, security (JWT), and integration tests with H2. |
| Layering | Claude | Explain the controller → service → repository pattern in Spring Boot and what belongs in each layer for a CRUD API. |
| JPA mapping | Cursor | Given a `users` table with username, email, fullName, role, and hashed password, sketch a `UserEntity` with correct `@Entity`, `@Table`, and validation annotations. |
| Auth model | Gemini | For JWT login on `/auth/login`, should credentials live on the user table or a separate auth table? Recommend one approach and why. |
| Error handling | Claude | How should a Spring Boot app return consistent JSON errors for 400, 401, 404, and 409 using `@ControllerAdvice`? |
| Testing | ChatGPT | What is the difference between `@WebMvcTest`, `@DataJpaTest`, and `@SpringBootTest`, and when should I use each for repository vs controller tests? |

---

## Planning

| Topic | Agent | Example prompt |
|--------|--------|----------------|
| Phased delivery | Claude | Given a README with Users, Auth, Projects, Tickets, Comments, then extended features (audit, attachments, CSV, soft delete), propose a phased implementation order with done-when criteria per phase. |
| Package layout | Cursor | Suggest a Java package structure for `com.att.tdp.issueflow` with controller, service, repository, model, dto, enums, exception, and config. |
| Data model | ChatGPT | From an issue-tracking API spec, list the database tables, entity names, foreign keys, and which REST endpoints use each table. |
| Cross-cutting | Gemini | Which features (JWT, audit log, optimistic locking, schedulers) should be introduced early vs late in the build plan? |
| Work plan doc | Cursor | Outline sections for a `WorkPlan.md` that tracks progress, test commands, and known fixes during incremental development. |

---

## REST design

| Topic | Agent | Example prompt |
|--------|--------|----------------|
| REST principles | ChatGPT | What are solid REST API design practices (resources, HTTP methods, status codes, idempotency), and what patterns are considered poor or non-RESTful? |
| Resource naming | Claude | Review these endpoints: `GET /users`, `POST /users/update/:id`, `PATCH /projects/:id`. Which paths and verbs match REST conventions, and what would you rename for consistency? |
| HTTP methods | Gemini | For create, full replace, partial update, and delete, when should I use POST vs PUT vs PATCH vs DELETE in a Spring `@RestController`? |
| Status codes | ChatGPT | Map typical outcomes to HTTP status codes: validation failure, duplicate username, missing resource, unauthorized, forbidden (ADMIN-only), and optimistic-lock conflict. |
| Request/response | Claude | Should API responses expose JPA entities directly or DTOs? Show a pattern for `CreateUserRequest`, `UpdateUserRequest`, and `UserResponse` without returning passwords. |
| Nested resources | Gemini | Comments belong to tickets. Compare `POST /comments` with body `ticketId` vs `POST /tickets/{ticketId}/comments`. Which is more RESTful and easier to secure? |
| Query parameters | ChatGPT | For `GET /tickets?projectId=1` and `GET /audit-logs?entityType=TICKET&entityId=5`, recommend naming and filtering conventions for list endpoints. |
| Soft delete | Claude | Design REST endpoints for soft-deleted tickets: hide from normal lists, `GET .../deleted` for admins, and `POST .../restore`. Which HTTP methods and paths fit best? |
| File upload | Gemini | For `POST /tickets/{id}/attachments` with multipart file, describe validation order (size, content-type) before reading bytes, and a sensible JSON metadata response. |
| Bulk operations | Gemini | For CSV export (`GET /tickets/export`) and import (`POST /tickets/import` multipart), recommend response shapes and error reporting for partial failures. |
| Pagination | ChatGPT | For `GET /users/{userId}/mentions?page=1&pageSize=20`, propose a consistent paginated response wrapper (`data`, `total`, `page`). |
| Security + REST | Claude | All routes except `POST /auth/login` require JWT. How should Spring Security map 401 vs 403, and where should role checks live (filter vs service)? |
| API contract | Cursor | Given a README API table (method, path, body, status, response), list checks to ensure controllers match the contract before writing tests. |
| Layer boundaries | Claude | A controller contains business logic and entity fields. Refactor guidance: thin controller, service owns rules, repository owns persistence—apply to a user CRUD slice. |

---

## Agent roles (summary)

| Agent | Typical use in this project |
|--------|------------------------------|
| **ChatGPT** | REST conventions, HTTP semantics, testing strategy, data-model brainstorming |
| **Claude** | Architecture, layering, error handling, security design, phased planning |
| **Gemini** | Auth/data-model tradeoffs, cross-cutting timing, nested-resource comparisons, upload-handler patterns |
| **Cursor** | Repo-aware planning, package layout, entity scaffolding, contract checks against `README.md` |

---

## How REST design prompts were applied

- Endpoints and bodies follow **`README.md`** as the API contract.
- Controllers delegate to services; services enforce ticket status rules, soft-delete, and role checks.
- DTOs separate JSON from JPA entities; errors go through **`GlobalExceptionHandler`** with appropriate status codes.
- Nested routes used where resources are owned (e.g. `/tickets/{id}/comments`, `/tickets/{id}/dependencies`).

---

## Related artifacts

| File | Role |
|------|------|
| `README.md` | API contract (paths, methods, bodies) |
| `WorkPlan.md` | Phased implementation and entity overview |
| `run.md` | Setup, run, and test instructions |
