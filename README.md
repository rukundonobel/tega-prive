# TEGA PRIVÉ

> **"YOUR JOURNEY. YOUR FREEDOM."**
>
> Inter-province transportation & ride-booking marketplace for Rwanda.
> Connects passengers travelling between Kigali and other provinces
> with verified, rated independent drivers.

---

## Status — All 7 phases complete ✅

| Phase | Deliverables | Tests |
|---|---|---|
| 1 — Foundation | JWT auth, RBAC (4 roles), Flyway, public homepage | 9 |
| 2 — Driver & vehicle | Driver verification, vehicle approval, Cloudinary/local storage | 15 |
| 3 — Journeys & booking | Journey search, overbooking-safe seat inventory, booking workflow | 23 |
| 4 — Payments & commission | MTN/Airtel abstraction, commission engine, driver earnings, payouts | 31 |
| 5 — Live GPS tracking | WebSocket/STOMP, authorised passenger map, freshness indicator | 36 |
| 6 — Administration | Full admin dashboard (11 sections), reporting, notifications, reviews, complaints | 47 |
| 7 — Security & deployment | Rate limiting, HMAC webhooks, production config, deployment docs | **52** |

**52 tests · 0 failures · Backend + frontend both build clean.**

---

## Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 21 · Spring Boot 4.2 · Spring Security + JWT · Spring Data JPA/Hibernate · WebSocket/STOMP · Maven · Flyway · PostgreSQL 15+ + PostGIS |
| **Frontend** | React 18 · TypeScript · Vite · Tailwind CSS · React Router · TanStack Query · Axios · React Hook Form · Zod · Lucide React · SockJS + STOMP |
| **Storage** | Local dev filesystem (default) · Cloudinary (production) |
| **Payments** | MTN Mobile Money · Airtel Money (pluggable provider abstraction) |
| **Maps** | OpenStreetMap iframe (default) · Google Maps Embed API (optional) |
| **Rate limiting** | Bucket4j in-process (token bucket, per-IP per-endpoint) |

---

## Project structure

```
tega-prive/
├── backend/                 Spring Boot modular monolith
│   ├── src/main/java/com/tegaprive/
│   │   ├── auth/            JWT auth, register, login, refresh
│   │   ├── users/           User entity + repository
│   │   ├── drivers/         Driver profiles, documents, verification
│   │   ├── vehicles/        Vehicle CRUD, photos, documents, approval
│   │   ├── journeys/        Locations, journeys, search
│   │   ├── bookings/        Booking workflow, seat inventory
│   │   ├── payments/        Payment providers, webhooks, commission
│   │   ├── commissions/     Commission rules, earnings ledger, payouts
│   │   ├── tracking/        GPS ingestion via WebSocket, authorisation
│   │   ├── notifications/   In-app notifications
│   │   ├── reviews/         Driver ratings
│   │   ├── complaints/      Complaint management
│   │   ├── administration/  Admin reporting, platform settings
│   │   ├── security/        JWT filter, rate limiter, RBAC
│   │   ├── storage/         StorageService abstraction (local/Cloudinary)
│   │   └── common/          BaseEntity, ApiResponse, AuditLog
│   ├── src/main/resources/
│   │   ├── application.yml       dev config
│   │   ├── application-prod.yml  production overrides
│   │   └── db/migration/         Flyway V1–V6
│   └── src/test/                 52 tests
├── frontend/                React SPA
│   └── src/
│       ├── pages/           public/ · driver/ · admin/ · tracking
│       ├── components/      UI system (Button, Input, Badge, Card…)
│       ├── layouts/         Public, Driver, Admin layouts
│       ├── services/        Axios API clients per domain
│       ├── hooks/           useTracking (STOMP WebSocket hooks)
│       ├── context/         AuthContext
│       ├── types/           Full TypeScript type catalogue
│       └── routes/          RequireAuth, RoleRoute guards
├── documentation/
│   ├── architecture.md      Full system design, ER diagram, decisions
│   ├── deployment.md        Server setup, Nginx, systemd, checklist
│   └── environment-variables.md  Complete env var reference
└── docker-compose.yml       PostgreSQL + PostGIS (requires Docker)
```

---

## Prerequisites

| Tool | Purpose | Notes |
|---|---|---|
| JDK 21+ | Backend runtime | Temurin recommended |
| Maven | **Not needed** — Maven Wrapper (`backend/mvnw`) is included | — |
| Node 20+ / npm | Frontend | nodejs.org |
| PostgreSQL 15+ **with PostGIS** | Database | See install guide below |
| Git | Version control | — |

### Installing PostgreSQL + PostGIS (Windows)

1. Install PostgreSQL 19 from https://www.postgresql.org/download/windows/
2. During installation, check **Stack Builder**
3. Launch Stack Builder → select your PostgreSQL install → expand **Spatial Extensions** → check **PostGIS 3.x** → install
4. In pgAdmin: create database `tegaprive`, then run `CREATE EXTENSION postgis;`

---

## Quick start

### 1. Database

```sql
CREATE DATABASE tegaprive;
\c tegaprive
CREATE EXTENSION IF NOT EXISTS postgis;
```

### 2. Backend

```powershell
# Set environment variables in your PowerShell session:
$env:TP_DB_HOST = "localhost"
$env:TP_DB_PORT = "5432"
$env:TP_DB_NAME = "tegaprive"
$env:TP_DB_USERNAME = "postgres"
$env:TP_DB_PASSWORD = "your_postgres_password"
$env:TP_JWT_SECRET = "tega-prive-dev-secret-must-be-32-bytes-minimum"
$env:TP_DEFAULT_ADMIN_EMAIL = "admin@tegaprive.rw"
$env:TP_DEFAULT_ADMIN_PASSWORD = "Admin1234!"

cd backend
.\mvnw.cmd spring-boot:run
```

API: http://localhost:8080  
Flyway runs migrations V1–V6 automatically.  
Bootstrap admin is created on first startup.

### 3. Frontend

```powershell
cd frontend
npm install
npm run dev
```

App: http://localhost:5173

---

## API overview

All responses use the `ApiResponse<T>` envelope:
```json
{ "success": true, "data": { ... }, "message": null, "errors": null }
```

| Group | Endpoints |
|---|---|
| Auth | `POST /api/auth/register` · `POST /api/auth/login` · `POST /api/auth/refresh` · `GET /api/auth/me` |
| Locations | `GET /api/locations` (public) |
| Journeys | `GET /api/journeys/search` (public) · `POST /api/journeys` (driver) · lifecycle endpoints |
| Bookings | `POST /api/bookings` · `GET /api/bookings/my` · cancel · driver decline |
| Payments | `POST /api/payments/initiate` · `POST /api/payments/{id}/dev-confirm` · webhooks |
| Commissions | Rules · earnings · payouts |
| Tracking | `GET /api/tracking/journey/{id}/state` (REST snapshot) · WS `/ws` |
| Notifications | `GET /api/notifications` · mark-read |
| Reviews | `POST /api/reviews` · `GET /api/reviews/driver/{id}` |
| Complaints | `POST /api/complaints` |
| Admin | `/api/admin/**` — all dashboard operations (ADMIN/SUPER_ADMIN only) |

---

## Payment flow

1. Passenger books → `POST /api/bookings` → status `PENDING_PAYMENT`
2. Passenger initiates payment → `POST /api/payments/initiate`
3. Provider sends push prompt to passenger's phone
4. Passenger approves → provider sends webhook → `POST /api/payments/webhook/mtn` or `/webhook/airtel`
5. Backend verifies HMAC signature → marks payment `SUCCESSFUL` → booking `CONFIRMED` → commission settled
6. **Dev mode**: skip step 3–4 by calling `POST /api/payments/{id}/dev-confirm`

---

## Live tracking flow

1. Driver starts journey → browser requests geolocation permission
2. Driver connects to WebSocket at `/ws` with JWT header
3. Driver publishes GPS fixes to `/app/journey/{id}/location`
4. Backend validates ownership, persists to `gps_locations`, broadcasts to `/topic/journey/{id}/location`
5. Passengers subscribed to that topic (CONFIRMED booking required) receive live position updates
6. Passenger tracking page: OSM iframe by default; Google Maps if `VITE_GOOGLE_MAPS_API_KEY` is set

---

## Security

- **JWT**: HS256, stateless (no server-side sessions). Access tokens: 30 min (15 min in prod). Refresh: 7 days.
- **RBAC**: `SUPER_ADMIN`, `ADMIN`, `DRIVER`, `PASSENGER`. Method-level `@PreAuthorize`.
- **Rate limiting**: Bucket4j token bucket per IP per endpoint group (login: 10/min, register: 5/min, payments: 20/min).
- **Webhook HMAC**: SHA-256 signature verification for MTN and Airtel callbacks. Timing-safe comparison.
- **Security headers**: `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`. HSTS ready (un-comment for production).
- **Passwords**: BCrypt.
- **GPS privacy**: driver location is never public — only passengers with a CONFIRMED booking can subscribe to a journey's tracking topic.

---

## Running tests

```powershell
# Backend (no PostgreSQL required — H2 in-memory)
cd backend
.\mvnw.cmd -B test

# Frontend (typecheck + build)
cd ../frontend
npm run build
```

Test summary:
- `AuthServiceTest` (5): register, duplicate prevention, login, bad credentials, token refresh
- `BookingServiceTest` (7): seat math, overbooking, duplicate booking, cancellation, payment status, driver decline, concurrent booking (6 threads)
- `DriverServiceTest` (4): registration, documents, admin approve/reject
- `JourneyServiceTest` (4): publish (approved vehicle only), search, lifecycle
- `CommissionEngineTest` (3): calculation (10%/3%), idempotency, driver earning
- `PaymentServiceTest` (4): initiate guard, dev-confirm, idempotency, webhook flow
- `TrackingServiceTest` (5): ingestion + broadcast, authorisation, inactive journey guard, passenger state
- `VehicleServiceTest` (8): full lifecycle including ownership and approval rules
- `WebhookSignatureVerifierTest` (3): valid HMAC, tampered payload, sha256 prefix
- `RateLimitFilterTest` (2): capacity enforcement, token tracking
- `JwtTokenProviderTest` (4): issue/parse, tamper detection, wrong secret
- `LocalDevStorageServiceTest` (3): store, unsupported type, empty file

---

## Deployment

See [`documentation/deployment.md`](documentation/deployment.md) for the complete production deployment guide.  
See [`documentation/environment-variables.md`](documentation/environment-variables.md) for all configuration variables.

---

## Integrations that need credentials

The following integrations are fully wired but require credentials you must obtain separately:

| Service | Where to get credentials | Env vars |
|---|---|---|
| Cloudinary | cloudinary.com → Dashboard → API Keys | `TP_CLOUDINARY_*` |
| MTN Mobile Money | momodeveloper.mtn.com → Sandbox | `TP_MTN_*` |
| Airtel Money | developers.airtel.africa | `TP_AIRTEL_*` |
| Google Maps Embed | console.cloud.google.com → Maps Embed API | `VITE_GOOGLE_MAPS_API_KEY` |
| Firebase FCM | console.firebase.google.com | Scaffolded, not yet wired |

Without any of these, the app works with safe fallbacks:
- Storage → local `uploads/` directory
- Payments → dev-bypass enabled
- Maps → OpenStreetMap iframe
