# TEGA PRIVÉ — Environment Variables Reference

All secrets and external service credentials are configured via environment variables.
**Never hard-code secrets. Never commit `.env` files.**

---

## Backend variables (`TP_` prefix)

### Database

| Variable | Required | Default | Description |
|---|---|---|---|
| `TP_DB_HOST` | Yes | `localhost` | PostgreSQL host |
| `TP_DB_PORT` | No | `5432` | PostgreSQL port |
| `TP_DB_NAME` | Yes | `tegaprive` | Database name |
| `TP_DB_USERNAME` | Yes | `postgres` | DB user |
| `TP_DB_PASSWORD` | Yes | `postgres` | DB password — **change in production** |

### JWT

| Variable | Required | Default | Description |
|---|---|---|---|
| `TP_JWT_SECRET` | Yes | dev default | HMAC signing secret — **minimum 32 bytes, 64+ in production** |
| `TP_JWT_ACCESS_EXPIRATION_MIN` | No | `30` | Access token lifetime in minutes |
| `TP_JWT_REFRESH_EXPIRATION_DAYS` | No | `7` | Refresh token lifetime in days |

### Bootstrap admin (first startup only)

| Variable | Required | Description |
|---|---|---|
| `TP_DEFAULT_ADMIN_EMAIL` | No | Email for bootstrap SUPER_ADMIN — created once if not exists |
| `TP_DEFAULT_ADMIN_PASSWORD` | No | Password for bootstrap admin |

### Storage

| Variable | Default | Description |
|---|---|---|
| `TP_STORAGE_PROVIDER` | `local` | `local` (dev) or `cloudinary` (production) |
| `TP_STORAGE_LOCAL_DIR` | `uploads` | Local upload directory (dev only) |
| `TP_CLOUDINARY_CLOUD_NAME` | — | Cloudinary cloud name |
| `TP_CLOUDINARY_API_KEY` | — | Cloudinary API key |
| `TP_CLOUDINARY_API_SECRET` | — | Cloudinary API secret |

### MTN Mobile Money

| Variable | Description |
|---|---|
| `TP_MTN_API_BASE_URL` | API base URL (sandbox: `https://sandbox.momodeveloper.mtn.com`) |
| `TP_MTN_SUBSCRIPTION_KEY` | Ocp-Apim-Subscription-Key from MTN developer portal |
| `TP_MTN_API_USER` | UUID of the API user created in sandbox |
| `TP_MTN_API_KEY` | API key for the API user |
| `TP_MTN_CALLBACK_HOST` | Public host for webhook callbacks (e.g. `https://api.tegaprive.rw`) |
| `TP_MTN_TARGET_ENVIRONMENT` | `sandbox` or `mtnrwanda` |
| `TP_MTN_WEBHOOK_SECRET` | HMAC secret for signature verification |

### Airtel Money

| Variable | Description |
|---|---|
| `TP_AIRTEL_API_BASE_URL` | API base URL (sandbox: `https://openapiuat.airtel.africa`) |
| `TP_AIRTEL_CLIENT_ID` | OAuth2 client ID |
| `TP_AIRTEL_CLIENT_SECRET` | OAuth2 client secret |
| `TP_AIRTEL_CALLBACK_HOST` | Public host for callbacks |
| `TP_AIRTEL_WEBHOOK_SECRET` | HMAC secret for signature verification |

### Payment general

| Variable | Default | Description |
|---|---|---|
| `TP_PAYMENT_DEV_BYPASS` | `true` | `true` enables `/api/payments/{id}/dev-confirm` — **set `false` in production** |

### CORS & server

| Variable | Default | Description |
|---|---|---|
| `TP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated allowed origins |
| `TP_SERVER_PORT` | `8080` | HTTP port the Spring Boot server listens on |

---

## Frontend variables (`VITE_` prefix)

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | _(empty — Vite proxy)_ | Backend API base URL. Leave empty in dev. Set `https://api.tegaprive.rw` in production. |
| `VITE_GOOGLE_MAPS_API_KEY` | _(empty — OSM fallback)_ | Google Maps Embed API key. Without it, OpenStreetMap iframe is used. |

---

## How Spring Boot loads environment variables

Spring Boot resolves `${TP_DB_HOST:localhost}` from:
1. JVM system properties (`-DTP_DB_HOST=...`)
2. OS environment variables
3. `.env` file loaded via the systemd `EnvironmentFile=` directive (see deployment guide)

The application **does not** auto-read `.env` files itself — that's handled by the OS/process manager.

---

## Generating a strong JWT secret

```bash
# Linux/macOS
openssl rand -hex 64

# Windows PowerShell
-join ((1..64) | ForEach-Object { '{0:x2}' -f (Get-Random -Max 256) })
```
