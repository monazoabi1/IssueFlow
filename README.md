# IssueFlow API

IssueFlow is a RESTful backend API for managing software projects and development tasks.

The system allows users to create and manage projects, tickets, comments, attachments, and ticket dependencies.
It also supports JWT-based authentication, user roles, audit logging, soft deletion, CSV import and export, user mentions, automatic ticket assignment, and overdue ticket escalation.

IssueFlow was developed using Java, Spring Boot, Spring Data JPA, Hibernate, PostgreSQL, and Docker.
The application follows a layered architecture that separates the controller, service, repository, and database responsibilities.

## Architecture

IssueFlow uses a layered backend architecture:

```text
Client
  |
  v
Controller Layer
  |
  v
Service Layer
  |
  v
Repository Layer
  |
  v
PostgreSQL Database
```



All protected endpoints require a JWT access token:

```http
Authorization: Bearer <token>
```

## Authentication

| Method | Endpoint       | Description                          |
| ------ | -------------- | ------------------------------------ |
| `POST` | `/auth/login`  | Log in and receive a JWT             |
| `POST` | `/auth/logout` | Invalidate the current token         |
| `GET`  | `/auth/me`     | Get the authenticated user's profile |

## Users

| Method   | Endpoint                   | Description                    |
| -------- | -------------------------- | ------------------------------ |
| `POST`   | `/users`                   | Create a user                  |
| `GET`    | `/users`                   | Get all users                  |
| `GET`    | `/users/{userId}`          | Get a user by ID               |
| `PATCH`  | `/users/{userId}`          | Update a user                  |
| `DELETE` | `/users/{userId}`          | Delete a user                  |
| `GET`    | `/users/{userId}/mentions` | Get comments mentioning a user |

## Projects

| Method   | Endpoint                         | Description                        |
| -------- | -------------------------------- | ---------------------------------- |
| `POST`   | `/projects`                      | Create a project                   |
| `GET`    | `/projects`                      | Get all active projects            |
| `GET`    | `/projects/{projectId}`          | Get a project by ID                |
| `PATCH`  | `/projects/{projectId}`          | Update a project                   |
| `DELETE` | `/projects/{projectId}`          | Soft-delete a project              |
| `GET`    | `/projects/deleted`              | Get deleted projects               |
| `POST`   | `/projects/{projectId}/restore`  | Restore a deleted project          |
| `GET`    | `/projects/{projectId}/workload` | Get developer workload information |

## Tickets

| Method   | Endpoint                          | Description                  |
| -------- | --------------------------------- | ---------------------------- |
| `POST`   | `/tickets`                        | Create a ticket              |
| `GET`    | `/tickets/{ticketId}`             | Get a ticket by ID           |
| `PATCH`  | `/tickets/{ticketId}`             | Update a ticket              |
| `DELETE` | `/tickets/{ticketId}`             | Soft-delete a ticket         |
| `GET`    | `/projects/{projectId}/tickets`   | Get all tickets in a project |
| `GET`    | `/tickets/deleted?projectId={id}` | Get deleted project tickets  |
| `POST`   | `/tickets/{ticketId}/restore`     | Restore a deleted ticket     |

## Comments

| Method   | Endpoint                       | Description             |
| -------- | ------------------------------ | ----------------------- |
| `POST`   | `/tickets/{ticketId}/comments` | Add a comment           |
| `GET`    | `/tickets/{ticketId}/comments` | Get all ticket comments |
| `PATCH`  | `/comments/{commentId}`        | Update a comment        |
| `DELETE` | `/comments/{commentId}`        | Delete a comment        |

## Ticket Dependencies

| Method   | Endpoint                                       | Description               |
| -------- | ---------------------------------------------- | ------------------------- |
| `POST`   | `/tickets/{ticketId}/dependencies`             | Add a blocking dependency |
| `GET`    | `/tickets/{ticketId}/dependencies`             | Get ticket dependencies   |
| `DELETE` | `/tickets/{ticketId}/dependencies/{blockerId}` | Remove a dependency       |

Example request:

```json
{
  "blockedBy": 42
}
```

## Attachments

| Method   | Endpoint                          | Description            |
| -------- | --------------------------------- | ---------------------- |
| `POST`   | `/tickets/{ticketId}/attachments` | Upload an attachment   |
| `GET`    | `/tickets/{ticketId}/attachments` | Get ticket attachments |
| `GET`    | `/attachments/{attachmentId}`     | Download an attachment |
| `DELETE` | `/attachments/{attachmentId}`     | Delete an attachment   |

Supported file types:

* `image/png`
* `image/jpeg`
* `application/pdf`
* `text/plain`

Maximum file size: `10 MB`.

## Ticket Import and Export

| Method | Endpoint                         | Description                    |
| ------ | -------------------------------- | ------------------------------ |
| `GET`  | `/tickets/export?projectId={id}` | Export project tickets to CSV  |
| `POST` | `/tickets/import`                | Import tickets from a CSV file |

The import request uses `multipart/form-data` and includes:

* `file`
* `projectId`

## Audit Logs

| Method | Endpoint                        | Description           |
| ------ | ------------------------------- | --------------------- |
| `GET`  | `/audit-logs`                   | Get all audit logs    |
| `GET`  | `/audit-logs?action={action}`   | Filter by action      |
| `GET`  | `/audit-logs?entityType={type}` | Filter by entity type |
| `GET`  | `/audit-logs?actorId={id}`      | Filter by actor       |

## Main API Values

### User Roles

```text
ADMIN
DEVELOPER
```

### Ticket Statuses

```text
TODO
IN_PROGRESS
IN_REVIEW
DONE
```

### Ticket Priorities

```text
LOW
MEDIUM
HIGH
CRITICAL
```

### Ticket Types

```text
BUG
FEATURE
TECHNICAL
```
