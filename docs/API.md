# Bowmenn API

Backend for **Bowmenn**, a logistics platform that connects shippers (customers) with
truck drivers. Built with Java 21, Spring Boot 3.5, Spring Security (JWT), Spring Data
JPA, PostgreSQL, and Flyway.

- **Interactive docs (Swagger UI):** http://localhost:8080/swagger-ui.html
- **OpenAPI spec (JSON):** http://localhost:8080/api-docs

---

## Table of contents

1. [Architecture](#architecture)
2. [Running locally](#running-locally)
3. [Configuration](#configuration)
4. [Authentication & roles](#authentication--roles)
5. [Response envelope](#response-envelope)
6. [Error handling](#error-handling)
7. [Domain model](#domain-model)
8. [Shipment status lifecycle](#shipment-status-lifecycle)
9. [Pricing](#pricing)
10. [Endpoint reference](#endpoint-reference)
11. [Smoke test](#smoke-test)
12. [Operational notes](#operational-notes)

---

## Architecture

Modular monolith — code is organized **by domain module**, not by layer. Each module
owns its controllers, services, repositories, DTOs, and entities.

```
com.bowmenn.bowmenn_api
├── common/
│   ├── config/       SecurityConfig, CorsConfig, OpenApiConfig
│   ├── exception/    GlobalExceptionHandler + typed exceptions
│   ├── response/     ApiResponse<T> envelope
│   ├── security/     JwtService, JwtAuthFilter, UserDetailsServiceImpl
│   ├── storage/      FileStorageService (Local + ImageKit implementations)
│   └── util/         PricingUtil (haversine, pricing, tracking numbers)
└── modules/
    ├── auth/         register / login / me
    ├── user/         User entity, UserRole, UserRepository, UserResponse
    ├── shipment/     Shipment, ShipmentStatus, TruckType, status logs, CRUD
    ├── driver/       driver-facing shipment actions
    ├── admin/        dashboard, assignment, user management, stats
    ├── pod/          proof-of-delivery upload & retrieval
    └── pricing/      public price estimation
```

Persistence uses **Hibernate `ddl-auto=validate`** — the schema is owned entirely by
Flyway migrations in `src/main/resources/db/migration`. The entities must match the
migrated schema or the app fails to start.

---

## Running locally

**Prerequisites:** JDK 21, a running PostgreSQL 13+ instance.

```bash
# 1. Create the database
createdb bowmenn            # or: psql -U postgres -c "CREATE DATABASE bowmenn;"

# 2. Run (from the bowmenn-api/ directory)
./mvnw spring-boot:run
```

On startup Flyway applies all migrations (`V1`–`V6`), including a seeded admin user.
The app listens on **port 8080** by default.

To package a jar: `./mvnw clean package` → `target/bowmenn-api-0.0.1-SNAPSHOT.jar`.

---

## Configuration

All settings are environment-overridable (defaults in parentheses).

| Env var | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/bowmenn` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB password |
| `JWT_SECRET` | (dev default, base64) | HMAC-SHA256 signing key — **override in production** |
| `JWT_EXPIRATION` | `86400000` | Token TTL in ms (24 h) |
| `STORAGE_PROVIDER` | `local` | `local` (disk, no credentials) or `imagekit` |
| `UPLOAD_DIR` | `uploads` | Local storage directory, served at `/uploads/**` |
| `IMAGEKIT_URL_ENDPOINT` | *(empty)* | Required when `STORAGE_PROVIDER=imagekit` |
| `IMAGEKIT_PUBLIC_KEY` | *(empty)* | Required when `STORAGE_PROVIDER=imagekit` |
| `IMAGEKIT_PRIVATE_KEY` | *(empty)* | Required when `STORAGE_PROVIDER=imagekit` — never commit |
| `IMAGEKIT_FOLDER` | `/bowmenn/pod` | Destination folder for POD images |
| `PORT` | `8080` | HTTP port |

---

## Authentication & roles

Authentication is **stateless JWT**. Register or log in to receive a token, then send it
on every protected request:

```
Authorization: Bearer <token>
```

The token payload carries `sub` (email), `role`, and `userId`, and expires per
`JWT_EXPIRATION`.

**Roles:** `CUSTOMER`, `DRIVER`, `ADMIN`.

| Path prefix | Access |
|---|---|
| `/api/auth/**` | public |
| `/swagger-ui/**`, `/api-docs/**` | public |
| `/api/shipments/**` | `CUSTOMER`, `ADMIN` |
| `/api/driver/**` | `DRIVER` |
| `/api/admin/**` | `ADMIN` |
| everything else | any authenticated user (e.g. `/api/pod/**`) |

**Role gates are not enough.** URL prefixes decide *which kind of user* may call an
endpoint; they cannot decide *which rows* that user may touch. Every shipment-scoped
operation additionally enforces object-level authorization:

| Operation | Permitted |
|---|---|
| View a shipment / its POD | the customer who booked it · its assigned driver · any admin |
| Update a shipment's status | its assigned driver · any admin |
| Upload proof of delivery | its assigned driver · any admin |

Anything else returns **403**. Without this, any customer could read any other customer's
shipment by guessing its id, and any driver could advance a job assigned to someone else.

**Seeded admin** (created by migration `V5`):

```
email:    admin@bowmenn.com
password: Admin@123
```

Admins cannot be created through `/api/auth/register` — that endpoint only accepts
`CUSTOMER` or `DRIVER`.

---

## Response envelope

Every JSON response uses a consistent envelope:

```json
{
  "status": "success",
  "message": "Human readable message",
  "data": { }
}
```

- `status` — `"success"` or `"error"`.
- `data` — the payload on success; `null` on most errors, or a field→message map for
  validation errors.

---

## Error handling

| Situation | HTTP | `message` |
|---|---|---|
| Resource not found | 404 | e.g. `Shipment not found` |
| Business-rule violation | 400 | e.g. `Cannot transition from IN_TRANSIT to ACCEPTED` |
| Bean-validation failure | 400 | `Validation failed` (+ `data` map of field errors) |
| Bad login credentials | 401 | `Invalid email or password` |
| Deactivated account login | 403 | `Account is deactivated` |
| Insufficient role / no token | 403 | (Spring Security) |
| Accessing another user's shipment/POD | 403 | `You do not have access to this shipment` |
| Acting on a shipment you're not assigned to | 403 | `You are not assigned to this shipment` |
| Unhandled server error | 500 | `An unexpected error occurred: …` |

Validation error example:

```json
{
  "status": "error",
  "message": "Validation failed",
  "data": {
    "password": "Password must be at least 6 characters",
    "email": "Invalid email format"
  }
}
```

---

## Domain model

**User** — `id`, `fullName`, `email` (unique), `phone`, `role`, `isActive`, timestamps.

**Shipment** — `id`, `trackingNumber` (unique, `BWN-YYYYMMDD-XXXXX`), `customer`,
`driver` (nullable), pickup/delivery address + lat/lng, `cargoDescription`,
`cargoWeight`, `truckType`, `status`, `estimatedDistanceKm`, `estimatedPrice`, `notes`,
timestamps.

**ShipmentStatusLog** — append-only audit row written on every status change
(`oldStatus`, `newStatus`, `changedBy`, `note`).

**ProofOfDelivery** — one per shipment: `imageUrl` (public URL), `imageFileId`
(storage-provider id, for later management/deletion), `note`, `uploadedBy`, `uploadedAt`.

**TruckType** — carries pricing/capacity:

| Type | Price/km (₦) | Capacity (kg) |
|---|---|---|
| `MINI` | 150 | 1,000 |
| `MEDIUM` | 250 | 5,000 |
| `LARGE` | 400 | 15,000 |

---

## Shipment status lifecycle

Transitions are enforced server-side by `ShipmentStatus.canTransitionTo`:

```
PENDING ──► ASSIGNED ──► ACCEPTED ──► PICKED_UP ──► IN_TRANSIT ──► DELIVERED
   │            │            │
   │            │            └──► CANCELLED
   │            ├──► REJECTED
   │            └──► CANCELLED
   └──► CANCELLED
```

- `PENDING → ASSIGNED | CANCELLED`
- `ASSIGNED → ACCEPTED | REJECTED | CANCELLED`
- `ACCEPTED → PICKED_UP | CANCELLED`
- `PICKED_UP → IN_TRANSIT`
- `IN_TRANSIT → DELIVERED`

Any other transition returns **400**. Uploading proof of delivery while a shipment is
`IN_TRANSIT` automatically advances it to `DELIVERED`.

---

## Pricing

Computed at shipment creation **only when all four coordinates are supplied**; otherwise
distance is `0` and price defaults to the ₦5,000 minimum.

- **Distance** — Haversine great-circle distance (km).
- **Price** — `max(distanceKm × truckType.pricePerKm × 1.10, 5000)`
  (base fare + 10% Bowmenn platform commission, floored at ₦5,000).

Example (Lagos → Ibadan, `MEDIUM`): ≈113.7 km → ₦31,265.76.

---

## Endpoint reference

Base URL: `http://localhost:8080`. All request/response bodies are JSON unless noted.
🔒 = requires `Authorization: Bearer <token>`.

### Authentication — `/api/auth`

#### `POST /api/auth/register`
Register a `CUSTOMER` or `DRIVER`. Returns a token + user.

```json
// request
{ "fullName": "Ada Cust", "email": "ada@test.com", "password": "secret1",
  "phone": "+2348010000000", "role": "CUSTOMER" }
```
```json
// 200
{ "status": "success", "message": "Registration successful",
  "data": { "token": "eyJ…", "tokenType": "Bearer",
            "user": { "id": "…", "fullName": "Ada Cust", "email": "ada@test.com",
                      "role": "CUSTOMER", "isActive": true, "createdAt": "…" } } }
```
Errors: `400` duplicate email, `400` role=ADMIN, `400` validation.

#### `POST /api/auth/login`
```json
// request
{ "email": "admin@bowmenn.com", "password": "Admin@123" }
```
Returns the same `AuthResponse` shape. `401` on bad credentials, `403` if deactivated.

#### `GET /api/auth/me` 🔒
Returns the current user (`UserResponse`).

---

### Shipments — Customer — `/api/shipments` 🔒 (`CUSTOMER`, `ADMIN`)

#### `POST /api/shipments`
Create a shipment. Coordinates optional (drive pricing when present).

```json
// request
{ "pickupAddress": "Lagos", "pickupLat": 6.5244, "pickupLng": 3.3792,
  "deliveryAddress": "Ibadan", "deliveryLat": 7.3775, "deliveryLng": 3.9470,
  "cargoDescription": "Electronics", "cargoWeight": 500.0,
  "truckType": "MEDIUM", "notes": "handle with care" }
```
Returns a `ShipmentResponse` with `status: "PENDING"`, generated `trackingNumber`,
`estimatedDistanceKm`, and `estimatedPrice`.

#### `GET /api/shipments/my`
List the caller's shipments (newest first).

#### `GET /api/shipments/{id}`
Fetch one shipment by UUID. `404` if missing.

#### `GET /api/shipments/track/{trackingNumber}`
Fetch by tracking number (e.g. `BWN-20260708-1454A`).

---

### Driver Portal — `/api/driver` 🔒 (`DRIVER`)

#### `GET /api/driver/shipments`
List shipments assigned to the calling driver.

#### `PUT /api/driver/shipments/{id}/accept`
`ASSIGNED → ACCEPTED`.

#### `PUT /api/driver/shipments/{id}/reject`
`ASSIGNED → REJECTED`.

#### `PUT /api/driver/shipments/{id}/status`
Generic transition. Body: `{ "status": "PICKED_UP", "note": "optional" }`.
`400` if the transition is not allowed.

---

### Admin Dashboard — `/api/admin` 🔒 (`ADMIN`)

#### `GET /api/admin/shipments`
All shipments (newest first).

#### `PUT /api/admin/shipments/{id}/assign`
Assign a driver; sets status to `ASSIGNED`.
Body: `{ "driverId": "<uuid>" }`. `400` if the user is not a `DRIVER`.

#### `PUT /api/admin/shipments/{id}/status`
Admin status transition. Body: `{ "status": "CANCELLED", "note": "…" }`.

#### `GET /api/admin/drivers` · `GET /api/admin/customers`
List all users of that role.

#### `PUT /api/admin/users/{id}/toggle-status`
Flip a user's `isActive`. Deactivated users cannot log in (403).

#### `GET /api/admin/stats`
```json
{ "totalShipments": 3, "totalDrivers": 2, "totalCustomers": 1,
  "pendingShipments": 0, "completedShipments": 1 }
```

---

### Proof of Delivery — `/api/pod` 🔒 (any authenticated user)

#### `POST /api/pod/{shipmentId}`  `multipart/form-data`
Upload a delivery photo. Fields: `file` (required), `note` (optional). The image is
uploaded to the configured storage provider and the returned public URL is stored on the
POD record. Advances the shipment `IN_TRANSIT → DELIVERED`. `400` if a POD already exists.

```bash
curl -X POST http://localhost:8080/api/pod/<shipmentId> \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@delivery.jpg" -F "note=Left with reception"
```
```json
// 200 — imageUrl points at the storage provider (local disk shown below)
{ "status": "success", "message": "POD uploaded",
  "data": { "id": "…", "shipmentId": "…",
            "imageUrl": "/uploads/<uuid>_delivery.jpg",
            "imageFileId": "…", "note": "Left with reception",
            "uploadedBy": "…", "uploadedAt": "…" } }
```

#### `GET /api/pod/{shipmentId}`
Retrieve POD metadata (`imageUrl`, `imageFileId`, `note`, `uploadedBy`, `uploadedAt`).
`404` if none.

---

## Smoke test

`scripts/smoke-test.sh` drives **every endpoint** end-to-end and asserts status codes and
response fields (64 checks). It uses unique timestamped emails, so it is safely
re-runnable against the same database.

```bash
# app must be running first
cd bowmenn-api
./mvnw spring-boot:run          # in one terminal
./scripts/smoke-test.sh         # in another

# against a different host / admin creds:
BASE_URL=https://api.example.com ADMIN_EMAIL=… ADMIN_PASSWORD=… ./scripts/smoke-test.sh
```

Requires `bash`, `curl`, `python3`. Exits `0` when all checks pass, non-zero otherwise
(with a list of failures). It covers: connectivity, the full auth flow (incl. negative
cases), shipment creation + pricing, the complete driver lifecycle, admin
assignment/cancellation/user-toggle, POD upload with auto-`DELIVERED`, duplicate-POD
rejection, RBAC (403/401) enforcement, and object-level authorization (IDOR regressions).

---

## Operational notes

- **Rotate `JWT_SECRET` in production.** The default key ships in the repo for local dev
  only.
- **POD image storage is pluggable** via `FileStorageService` (`common/storage`).
  `STORAGE_PROVIDER=local` (default) writes to `UPLOAD_DIR` and serves the files at
  `/uploads/**` — no credentials, works straight after a clone, but the disk is not shared
  across instances. `STORAGE_PROVIDER=imagekit` uploads to ImageKit (server-side, HTTP Basic
  auth) and returns absolute CDN URLs; use this in production. Adding S3/GCS is a new
  implementation, not a refactor. **No credentials are committed** — supply `IMAGEKIT_*` via
  environment variables.
- **Changing an already-applied migration** (e.g. editing `V5`) will fail Flyway's
  checksum validation on existing databases. Add a new `V6+` migration instead, or reset
  the dev database.
- `GET /api/auth/me` is under the public `/api/auth/**` matcher; call it **with** a token.
  A tokenless call has no principal to resolve.
- Static analysis warnings you may see at startup (`open-in-view`, explicit dialect,
  `UserDetailsService` bean) are benign and do not affect behavior.
