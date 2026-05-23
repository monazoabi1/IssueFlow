# IssueFlow — Setup and Run Guide

This document describes how to install dependencies, start PostgreSQL, build the project, run the Spring Boot application, and execute the test suite.

| Item | Value |
|------|--------|
| Stack | Spring Boot 3.4, Java 21, Maven, PostgreSQL 16 (Docker) |
| API base URL | `http://localhost:8080` |
| Database (runtime) | `jdbc:postgresql://localhost:5432/issueflow` |
| Database (tests) | H2 in-memory (`src/test/resources/application.yaml`) |

**Related files:** API contract → `README.md` · implementation plan → `WorkPlan.md`

---

## Prerequisites

Install these before following the steps below:

| Tool | Version | Purpose |
|------|---------|---------|
| **JDK** | 21 | Required by `pom.xml` (`java.version`) |
| **Docker Desktop** (or Docker Engine + Compose) | Current | Runs PostgreSQL via `compose.yml` |
| **Git** (optional) | Any | Clone the repository |

Maven is **not** required separately; the project includes the Maven Wrapper (`mvnw` / `mvnw.cmd`).

Verify Java 21:

```powershell
java -version
```

Expected output includes `version "21.x.x"`. JDK 25+ is not supported for this project (Lombok compile issues).

---

## Quick start (full workflow)

From the project root (`issueflow-java/`):

```powershell
# 1 — Dependencies (first time only)
.\mvnw.cmd -version

# 2 — Database
docker compose -f compose.yml up -d

# 3 — Build
.\mvnw.cmd clean package -DskipTests

# 4 — Run app (PostgreSQL must be up)
.\mvnw.cmd spring-boot:run

# 5 — Tests (in another terminal; Docker not required)
.\mvnw.cmd test
```

On Linux or macOS, replace `.\mvnw.cmd` with `./mvnw`.

---

## 1. Install dependencies

All Java libraries are declared in `pom.xml` and downloaded automatically by Maven.

### Step 1.1 — Open a terminal in the project root

```powershell
cd path\to\issueflow-java
```

### Step 1.2 — Set Java 21 (if multiple JDKs are installed)

**Windows (PowerShell)** — run before any Maven command if `java -version` is not 21:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
```

Adjust the path to match your JDK 21 installation. To persist `JAVA_HOME` for your user account:

```powershell
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21.0.11", "User")
```

**Linux / macOS:**

```bash
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
```

### Step 1.3 — Download Maven dependencies

The wrapper downloads Maven 3.9.x on first use, then resolves all project JARs:

```powershell
.\mvnw.cmd -version
.\mvnw.cmd dependency:resolve
```

**Success:** `Apache Maven 3.9.x` and `Java version: 21.x.x` appear in the output; dependencies are cached under `%USERPROFILE%\.m2\repository` (Windows) or `~/.m2/repository` (Unix).

### Step 1.4 — Install Docker (for PostgreSQL only)

1. Install [Docker Desktop](https://docs.docker.com/desktop/) (Windows/macOS) or Docker Engine + Compose (Linux).
2. Start Docker and wait until it reports **running**.
3. Confirm:

```powershell
docker --version
docker compose version
```

Tests do **not** need Docker; only running the application against PostgreSQL does.

---

## 2. Start the database

PostgreSQL is defined in `compose.yml` at the project root.

| Setting | Value |
|---------|--------|
| Image | `postgres` (latest tag pulled on first run) |
| Host port | `5432` |
| Database | `issueflow` |
| Username | `issueflow` |
| Password | `issueflow` |

These match `src/main/resources/application.yaml`.

### Step 2.1 — Start PostgreSQL in the background

**Windows (PowerShell):**

```powershell
cd path\to\issueflow-java
docker compose -f compose.yml up -d
```

**Linux / macOS:**

```bash
cd path/to/issueflow-java
docker compose -f compose.yml up -d
```

### Step 2.2 — Verify the container is running

```powershell
docker compose -f compose.yml ps
```

**Success:** a service named `db` (container name like `issueflow-java-db-1`) shows state **running** and port `0.0.0.0:5432->5432/tcp`.

### Step 2.3 — Stop the database (when finished)

```powershell
docker compose -f compose.yml down
```

**Note:** PostgreSQL is not an HTTP server. Do not open `http://localhost:5432` in a browser.

On first application start, Hibernate `ddl-auto: update` creates/updates tables automatically; no manual SQL migration is required.

---

## 3. Build the project

Build compiles sources and packages a runnable JAR under `target/`.

### Step 3.1 — Clean compile and package

```powershell
cd path\to\issueflow-java
.\mvnw.cmd clean package
```

To build without running tests (faster):

```powershell
.\mvnw.cmd clean package -DskipTests
```

### Step 3.2 — Confirm success

**Success indicators:**

- Console ends with `BUILD SUCCESS`
- JAR exists: `target\issueflow-0.0.1-SNAPSHOT.jar` (Windows) or `target/issueflow-0.0.1-SNAPSHOT.jar` (Unix)

### Step 3.3 — Compile only (optional)

```powershell
.\mvnw.cmd clean compile
```

---

## 4. Run the application

The app connects to PostgreSQL on `localhost:5432`. **Start the database (section 2) before starting the app.**

### Step 4.1 — Start PostgreSQL (if not already running)

```powershell
docker compose -f compose.yml up -d
docker compose -f compose.yml ps
```

### Step 4.2 — Run with Maven

```powershell
cd path\to\issueflow-java
.\mvnw.cmd spring-boot:run
```

**Alternative — run the packaged JAR:**

```powershell
java -jar target\issueflow-0.0.1-SNAPSHOT.jar
```

### Step 4.3 — Confirm the server is up

**Success indicators in the log:**

- `Starting IssueFlowApplication using Java 21...`
- `Tomcat started on port 8080`

Open or call:

```text
http://localhost:8080
```

Most endpoints require a JWT except `POST /auth/login`. See `README.md` for the full API.

### Step 4.4 — Stop the application

Press `Ctrl+C` in the terminal running Spring Boot.

---

## 5. Run the tests

Automated tests use an **H2 in-memory** database configured in `src/test/resources/application.yaml`. **Docker and PostgreSQL are not required** for `mvn test`.

### Step 5.1 — Run the full test suite

```powershell
cd path\to\issueflow-java
.\mvnw.cmd test
```

**Success:** console ends with `BUILD SUCCESS` and `Tests run: N, Failures: 0, Errors: 0`.

Reports are written to:

```text
target/surefire-reports/
```

### Step 5.2 — Run a single test class

```powershell
.\mvnw.cmd test -Dtest=UserRepositoryTest
```

Multiple classes (comma-separated, no spaces):

```powershell
.\mvnw.cmd test "-Dtest=AuthControllerTest,UserServiceTest,TicketControllerTest"
```

### Step 5.3 — Run tests by package

```powershell
# All controller tests
.\mvnw.cmd test -Dtest=com.att.tdp.issueflow.controller.*Test

# All repository tests
.\mvnw.cmd test -Dtest=com.att.tdp.issueflow.repository.*Test

# Stress / concurrency tests (slower)
.\mvnw.cmd test -Dtest=com.att.tdp.issueflow.stress.*Test
```

### Test layout (overview)

| Package | Examples |
|---------|----------|
| `repository` | `UserRepositoryTest`, `TicketRepositoryTest` |
| `service` | `UserServiceTest`, `TicketServiceTest`, `AuditLogServiceTest` |
| `controller` | `AuthControllerTest`, `TicketControllerTest` |
| `validation` | `AttachmentContentValidatorTest`, `TicketCsvValidatorTest` |
| `stress` | `ConcurrentTicketUpdateStressTest`, `SoftDeleteRestoreStressTest` |
| (root) | `IssueFlowApplicationTests` — Spring context smoke test |

---

## Troubleshooting

| Problem | Cause | Fix |
|---------|--------|-----|
| `Connection to localhost:5432 refused` | PostgreSQL not running | `docker compose -f compose.yml up -d` and verify with `docker compose ps` |
| `java -version` shows 25 (or wrong JDK) | `PATH` / `JAVA_HOME` points elsewhere | Set `JAVA_HOME` to JDK 21 and put `%JAVA_HOME%\bin` first on `PATH` (section 1.2) |
| Lombok / compile errors on JDK 25 | Project targets Java 21 | Use JDK 21 only |
| `docker: command not found` | Docker not installed or terminal started before Docker Desktop | Install/start Docker; open a new terminal |
| Port 5432 already in use | Another PostgreSQL instance | Stop the other service or change the published port in `compose.yml` and `application.yaml` |
| Port 8080 already in use | Another process on 8080 | Stop it or set `server.port` in `application.yaml` |
| Tests pass locally but build is slow | Stress tests + SQL logging | Normal; stress tests exercise concurrency |

**Windows `PATH` note:** If Oracle `javapath` appears before your JDK 21 folder, remove `javapath` from the effective path or always prepend `$env:JAVA_HOME\bin` in the session (section 1.2).

---

## Configuration reference

| File | Used when |
|------|-----------|
| `src/main/resources/application.yaml` | Running the app (PostgreSQL) |
| `src/test/resources/application.yaml` | `mvn test` (H2) |
| `compose.yml` | Docker PostgreSQL |

JWT and upload limits are also defined in the main `application.yaml` (`issueflow.jwt.*`, multipart 10 MB max).

---

## Related documentation

| Document | Contents |
|----------|----------|
| `README.md` | REST API endpoints, request/response bodies, status codes |
| `WorkPlan.md` | Phased implementation notes and entity overview |
| `prompts.md` | AI prompts used during development |
