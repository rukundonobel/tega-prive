# TEGA PRIVÉ — Deployment Guide

This document describes how to deploy TEGA PRIVÉ to a production Ubuntu/Debian server.
The setup uses a **Spring Boot JAR** served by **systemd** behind an **Nginx** TLS reverse
proxy, and a **Vite static build** served directly by Nginx.

> Prerequisites: Ubuntu 22.04 LTS, Java 21+, PostgreSQL 15+ with PostGIS, Nginx, Certbot.

---

## 1. Server preparation

```bash
# System update
sudo apt update && sudo apt upgrade -y

# Java 21
sudo apt install -y openjdk-21-jre-headless

# PostgreSQL 15 + PostGIS
sudo apt install -y postgresql-15 postgresql-15-postgis-3

# Nginx
sudo apt install -y nginx certbot python3-certbot-nginx

# Create a dedicated system user (no login shell, no home dir)
sudo useradd --system --no-create-home --shell /bin/false tegaprive
```

---

## 2. Database setup

```sql
-- As postgres superuser:
CREATE DATABASE tegaprive;
CREATE USER tegaprive_app WITH ENCRYPTED PASSWORD 'CHANGE_THIS_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON DATABASE tegaprive TO tegaprive_app;

\c tegaprive
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO tegaprive_app;
```

---

## 3. Build the application

**On your development machine:**

```powershell
# Backend — produce a self-contained JAR
cd backend
.\mvnw.cmd -B -q clean package -DskipTests
# Output: backend/target/backend-0.0.1-SNAPSHOT.jar

# Frontend — produce static files
cd ../frontend
npm ci
npm run build
# Output: frontend/dist/
```

---

## 4. Deploy the backend JAR

```bash
# Create the deployment directory
sudo mkdir -p /opt/tegaprive
sudo chown tegaprive:tegaprive /opt/tegaprive

# Create log directory
sudo mkdir -p /var/log/tegaprive
sudo chown tegaprive:tegaprive /var/log/tegaprive

# Create uploads directory (dev only — production uses Cloudinary)
sudo mkdir -p /opt/tegaprive/uploads
sudo chown tegaprive:tegaprive /opt/tegaprive/uploads

# Copy the JAR
sudo cp backend/target/backend-0.0.1-SNAPSHOT.jar /opt/tegaprive/app.jar
sudo chown tegaprive:tegaprive /opt/tegaprive/app.jar
```

---

## 5. Environment configuration

Create the environment file (never commit this file):

```bash
sudo nano /opt/tegaprive/.env
# Restrict permissions: only the tegaprive user can read it
sudo chmod 600 /opt/tegaprive/.env
sudo chown tegaprive:tegaprive /opt/tegaprive/.env
```

Paste and fill every variable:

```bash
# ── Database ─────────────────────────────────────────────────────
TP_DB_HOST=localhost
TP_DB_PORT=5432
TP_DB_NAME=tegaprive
TP_DB_USERNAME=tegaprive_app
TP_DB_PASSWORD=CHANGE_THIS_STRONG_PASSWORD

# ── JWT — generate with: openssl rand -hex 64 ─────────────────────
TP_JWT_SECRET=REPLACE_WITH_64_CHAR_RANDOM_HEX
TP_JWT_ACCESS_EXPIRATION_MIN=15
TP_JWT_REFRESH_EXPIRATION_DAYS=7

# ── Bootstrap admin (created on first startup only) ───────────────
TP_DEFAULT_ADMIN_EMAIL=admin@tegaprive.rw
TP_DEFAULT_ADMIN_PASSWORD=CHANGE_THIS_STRONG_PASSWORD

# ── Storage ──────────────────────────────────────────────────────
TP_STORAGE_PROVIDER=cloudinary
TP_CLOUDINARY_CLOUD_NAME=your_cloud_name
TP_CLOUDINARY_API_KEY=your_api_key
TP_CLOUDINARY_API_SECRET=your_api_secret

# ── Payments ─────────────────────────────────────────────────────
TP_PAYMENT_DEV_BYPASS=false
TP_MTN_API_BASE_URL=https://sandbox.momodeveloper.mtn.com
TP_MTN_SUBSCRIPTION_KEY=your_subscription_key
TP_MTN_API_USER=your_api_user_uuid
TP_MTN_API_KEY=your_api_key
TP_MTN_CALLBACK_HOST=https://api.tegaprive.rw
TP_MTN_TARGET_ENVIRONMENT=mtnrwanda
TP_MTN_WEBHOOK_SECRET=REPLACE_WITH_MTN_WEBHOOK_SECRET

TP_AIRTEL_API_BASE_URL=https://openapi.airtel.africa
TP_AIRTEL_CLIENT_ID=your_client_id
TP_AIRTEL_CLIENT_SECRET=your_client_secret
TP_AIRTEL_CALLBACK_HOST=https://api.tegaprive.rw
TP_AIRTEL_WEBHOOK_SECRET=REPLACE_WITH_AIRTEL_WEBHOOK_SECRET

# ── CORS ─────────────────────────────────────────────────────────
TP_CORS_ALLOWED_ORIGINS=https://tegaprive.rw,https://www.tegaprive.rw

# ── Server ───────────────────────────────────────────────────────
TP_SERVER_PORT=8080
```

---

## 6. systemd service

```bash
sudo nano /etc/systemd/system/tegaprive.service
```

```ini
[Unit]
Description=TEGA PRIVÉ Backend
Documentation=https://github.com/your-org/tega-prive
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=tegaprive
Group=tegaprive
WorkingDirectory=/opt/tegaprive

# Load all environment variables from the env file
EnvironmentFile=/opt/tegaprive/.env

ExecStart=/usr/bin/java \
  -Xms256m \
  -Xmx1g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=prod \
  -jar /opt/tegaprive/app.jar

# Restart on failure
Restart=on-failure
RestartSec=10
StartLimitInterval=60
StartLimitBurst=3

# Security hardening
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/tegaprive /var/log/tegaprive
PrivateTmp=true
PrivateDevices=true

StandardOutput=journal
StandardError=journal
SyslogIdentifier=tegaprive

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable tegaprive
sudo systemctl start tegaprive

# Check it started successfully
sudo systemctl status tegaprive
sudo journalctl -u tegaprive -f --no-pager
```

---

## 7. Deploy the frontend

```bash
# Copy the Vite build output to Nginx web root
sudo mkdir -p /var/www/tegaprive
sudo cp -r frontend/dist/* /var/www/tegaprive/
sudo chown -R www-data:www-data /var/www/tegaprive
```

---

## 8. Nginx configuration

```bash
sudo nano /etc/nginx/sites-available/tegaprive
```

```nginx
# ─────────────────────────────────────────────
# TEGA PRIVÉ — Nginx configuration
# ─────────────────────────────────────────────

# Redirect HTTP → HTTPS
server {
    listen 80;
    server_name tegaprive.rw www.tegaprive.rw api.tegaprive.rw;
    return 301 https://$host$request_uri;
}

# ── Frontend SPA ─────────────────────────────
server {
    listen 443 ssl http2;
    server_name tegaprive.rw www.tegaprive.rw;

    ssl_certificate     /etc/letsencrypt/live/tegaprive.rw/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/tegaprive.rw/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    root /var/www/tegaprive;
    index index.html;

    # Security headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' https://maps.googleapis.com; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' wss://api.tegaprive.rw https://api.tegaprive.rw;" always;

    # Cache static assets aggressively; never cache index.html
    location ~* \.(js|css|png|svg|ico|woff2?)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # React Router — all routes serve index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Gzip
    gzip on;
    gzip_vary on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
}

# ── Backend API ──────────────────────────────
server {
    listen 443 ssl http2;
    server_name api.tegaprive.rw;

    ssl_certificate     /etc/letsencrypt/live/api.tegaprive.rw/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.tegaprive.rw/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;

    # Security headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;

    # Proxy to Spring Boot
    location / {
        proxy_pass         http://127.0.0.1:8080;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 90;
    }

    # WebSocket upgrade for live GPS tracking (/ws)
    location /ws {
        proxy_pass         http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade    $http_upgrade;
        proxy_set_header   Connection "upgrade";
        proxy_set_header   Host       $host;
        proxy_read_timeout 86400;   # 24h — keep WS connections alive
    }

    # Upload size limit
    client_max_body_size 20m;
}
```

```bash
# Enable the site
sudo ln -s /etc/nginx/sites-available/tegaprive /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# TLS certificates (replace with your actual domain)
sudo certbot --nginx -d tegaprive.rw -d www.tegaprive.rw -d api.tegaprive.rw
```

---

## 9. Firewall

```bash
sudo ufw allow ssh
sudo ufw allow 'Nginx Full'    # ports 80 + 443
sudo ufw deny 8080             # block direct Spring Boot access from outside
sudo ufw enable
sudo ufw status
```

---

## 10. Zero-downtime redeployment

```bash
# 1. Build new JAR on dev machine, copy to server
scp backend/target/backend-0.0.1-SNAPSHOT.jar user@your-server:/opt/tegaprive/app-new.jar

# 2. On server: swap atomically and restart
sudo mv /opt/tegaprive/app-new.jar /opt/tegaprive/app.jar
sudo chown tegaprive:tegaprive /opt/tegaprive/app.jar
sudo systemctl restart tegaprive

# 3. Watch logs for successful startup
sudo journalctl -u tegaprive -f --no-pager

# Frontend: replace static files (no downtime)
sudo cp -r dist/* /var/www/tegaprive/
```

---

## 11. Health check

```bash
# Backend health
curl -s https://api.tegaprive.rw/api/auth/me          # should return 401 (requires JWT)
curl -s https://api.tegaprive.rw/api/locations         # should return 200 with locations list

# Check logs
sudo journalctl -u tegaprive --since "10 minutes ago"
```

---

## 12. Production checklist

Before going live, verify every item:

### Security
- [ ] `TP_JWT_SECRET` is at least 64 chars, randomly generated (`openssl rand -hex 64`)
- [ ] `TP_DEFAULT_ADMIN_PASSWORD` is strong and changed immediately after first login
- [ ] `TP_DB_PASSWORD` is strong
- [ ] `TP_PAYMENT_DEV_BYPASS=false`
- [ ] `TP_MTN_WEBHOOK_SECRET` and `TP_AIRTEL_WEBHOOK_SECRET` are set
- [ ] All `.env` files have `600` permissions
- [ ] Port 8080 is blocked externally (only Nginx accesses it)
- [ ] TLS certificates are valid and auto-renewed (`certbot renew --dry-run`)
- [ ] HSTS header is active (un-comment the line in `SecurityHeadersFilter`)

### Database
- [ ] PostgreSQL is running and PostGIS is installed
- [ ] Flyway migrations ran successfully (check the log for "Successfully applied N migrations")
- [ ] Bootstrap super-admin was created (`[TEGA PRIVÉ] Bootstrap super-admin created`)
- [ ] Database backups are configured (pg_dump cron job)

### Storage
- [ ] `TP_STORAGE_PROVIDER=cloudinary` with real Cloudinary credentials
- [ ] Upload size limits match Nginx `client_max_body_size`

### Payments
- [ ] MTN and Airtel callback URLs (`TP_MTN_CALLBACK_HOST`, `TP_AIRTEL_CALLBACK_HOST`) point to `https://api.tegaprive.rw`
- [ ] MTN/Airtel webhook endpoints are registered in the respective developer portals
- [ ] Sandbox tested end-to-end before switching to production keys

### Performance
- [ ] JVM heap size is appropriate for the server RAM (`-Xmx1g` for a 2 GB server)
- [ ] PostgreSQL connection pool size matches Hikari `maximum-pool-size`
- [ ] Nginx gzip is enabled

### Monitoring
- [ ] `sudo journalctl -u tegaprive -f` shows no errors after startup
- [ ] Log rotation is configured (`logrotate` for `/var/log/tegaprive/`)
- [ ] Set up uptime monitoring (e.g. UptimeRobot pointing at `https://api.tegaprive.rw/api/locations`)
