# Bowmenn API

Backend for **Bowmenn**, a logistics platform that connects shippers (customers) with
truck drivers. Customers create shipments, admins assign drivers, drivers move shipments
through a delivery lifecycle, and drivers upload proof of delivery — all over a JSON REST
API secured with JWT.

> **Full API reference:** [`docs/API.md`](docs/API.md) ·
> **Interactive docs:** http://localhost:8080/swagger-ui.html (when running)

---

## Tech stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5.16 (Web, Security, Data JPA, Validation, Actuator) |
| Database | PostgreSQL |
| Migrations | Flyway (`V1`–`V6`) |
| Auth | Stateless JWT (jjwt 0.11.5) |
| File storage | Pluggable: local disk (default) or ImageKit |
| API docs | springdoc-openapi 2.8.9 (Swagger UI) |
| Build | Maven (wrapper included) |

---

## Quick start

**Prerequisites:** JDK 21 and a running PostgreSQL instance.

```bash
# 1. Create the database
createdb bowmenn        # or: psql -U postgres -c "CREATE DATABASE bowmenn;"

# 2. Run (from this directory)
./mvnw spring-boot:run
```

On startup Flyway applies all migrations and seeds an admin user. The app listens on
**http://localhost:8080**.

**Seeded admin:** `admin@bowmenn.com` / `Admin@123`

Then open the interactive docs at **http://localhost:8080/swagger-ui.html**, click
**Authorize**, and paste a JWT obtained from `POST /api/auth/login`.

---

## Project layout

Modular monolith — organized **by domain**, not by layer. Each module owns its
controllers, services, repositories, DTOs, and entities.

```
src/main/java/com/bowmenn/bowmenn_api
├── common/           cross-cutting: config, exception handling, response envelope,
│                     security (JWT), storage (local/ImageKit), pricing util
└── modules/
    ├── auth/         register / login / me
    ├── user/         user entity, roles, repository
    ├── shipment/     shipments, status lifecycle, truck types, status audit log
    ├── driver/       driver-facing shipment actions
    ├── admin/        dashboard, driver assignment, user management, stats
    ├── pod/          proof-of-delivery upload & retrieval
    └── pricing/      public price estimation

src/main/resources
├── application.properties
└── db/migration/     V1..V6 Flyway migrations
```

The schema is owned entirely by Flyway; Hibernate runs in `validate` mode.

---

## Roles & endpoints

Three roles — `CUSTOMER`, `DRIVER`, `ADMIN` — gate access by URL prefix:

| Prefix | Access | Purpose |
|---|---|---|
| `/api/auth/**` | public | register, login, current user |
| `/api/shipments/**` | `CUSTOMER`, `ADMIN` | create / view / track shipments |
| `/api/driver/**` | `DRIVER` | accept, reject, update assigned shipments |
| `/api/admin/**` | `ADMIN` | assign drivers, manage users, stats |
| `/api/pod/**` | authenticated | upload / fetch proof of delivery |
| `/swagger-ui/**`, `/api-docs/**` | public | API documentation |

See [`docs/API.md`](docs/API.md) for every endpoint with request/response examples, the
shipment status state machine, and the pricing formula.

---

## Configuration

All settings are environment-overridable (see [`docs/API.md`](docs/API.md#configuration)
for the full table). Key ones:

| Env var | Default | Notes |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/bowmenn` | |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `postgres` / `postgres` | |
| `JWT_SECRET` | dev default | **override in production** |
| `STORAGE_PROVIDER` | `local` | `local` or `imagekit` |
| `IMAGEKIT_PRIVATE_KEY` | *(empty)* | only when `STORAGE_PROVIDER=imagekit` |
| `PORT` | `8080` | |

> ⚠️ `JWT_SECRET` has a development-only default in `application.properties`. **Override it
> in any real environment.** No third-party credentials are committed: storage defaults to
> local disk, and ImageKit keys are supplied via environment variables only.
> See [`.env.example`](.env.example).

---

## Testing

`scripts/smoke-test.sh` drives **every endpoint** end-to-end and asserts status codes and
response fields (64 checks), including negative cases and RBAC enforcement. It uses unique
timestamped emails, so it is safely re-runnable.

```bash
./mvnw spring-boot:run        # terminal 1
./scripts/smoke-test.sh       # terminal 2

# against another host / admin creds:
BASE_URL=https://api.example.com ADMIN_EMAIL=… ADMIN_PASSWORD=… ./scripts/smoke-test.sh
```

Requires `bash`, `curl`, `python3`. Exits non-zero if any check fails.

Unit/integration tests: `./mvnw test`.

---

## Build

```bash
./mvnw clean package          # -> target/bowmenn-api-0.0.1-SNAPSHOT.jar
java -jar target/bowmenn-api-0.0.1-SNAPSHOT.jar
```
