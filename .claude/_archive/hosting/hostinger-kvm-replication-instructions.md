# CPSS Deployment Replication Instructions

These instructions replicate a CPSS deployment on a fresh Ubuntu server: MySQL, the Spring Boot
jar under systemd, the built frontend behind Nginx, and n8n for the database-rebuild webhook.

> **Adapted 2026-08-12** from a runbook written for a different Spring Boot + React + MySQL app
> on the same hosting shape (Hostinger KVM, Ubuntu, Nginx, systemd). Paths, database names, the
> service unit, and the table list have been retargeted to CPSS. The steps themselves are
> stack-generic and were not otherwise altered — they have **not** been re-executed end to end
> against a fresh CPSS box, so treat unverified specifics (jar size, exact artifact names) as
> approximate.
>
> **This describes the VPS/self-hosted path.** CPSS's primary documented deployment target is
> AWS Elastic Beanstalk (`../DEPLOYMENT.md`), and there are CPSS-native VPS guides alongside this
> one — `qa-setup.md` (provisioning), `qa-hosting.md` (operations), `qa-deployment.md` (deploys).
> Prefer those; this file is the from-scratch replication variant.

---

## Prerequisites

- Fresh Ubuntu server (22.04+ recommended)
- Root/sudo access
- The following artifacts copied from the source machine:
  - The application jar → deployed to `/opt/cpss/cpss-server.jar`. Built with
    `./gradlew buildDeployment`, which bundles the frontend into the jar. Current project version
    is `0.0.2-SNAPSHOT` (`build.gradle`).
  - A frontend build zip — **only if** serving the SPA from Nginx separately rather than from
    inside the jar. See the note at Step 7.
  - A database backup SQL file (latest from `/opt/cpss/db_backup/`) — optional; see Step 3c.
  - The `.env` file (with credentials updated for the new machine)

---

## Step 1: Install System Packages

```bash
apt update && apt upgrade -y
apt install -y nginx mysql-server openjdk-21-jre-headless docker.io unzip curl
systemctl enable docker && systemctl start docker
```

## Step 2: Create Directory Structure

```bash
mkdir -p /opt/cpss/releases
mkdir -p /opt/cpss/db_backup
mkdir -p /var/www/cpss
mkdir -p /var/log/cpss
mkdir -p /opt/n8n
```

## Step 3: Configure MySQL

### 3a. Edit MySQL config to bind to all interfaces

Edit `/etc/mysql/mysql.conf.d/mysqld.cnf` and set:

```ini
bind-address = 0.0.0.0
mysqlx-bind-address = 127.0.0.1
key_buffer_size = 16M
max_binlog_size = 100M
```

### 3b. Restart MySQL and create the database + user

```bash
systemctl restart mysql
systemctl enable mysql
```

```sql
-- Run via: mysql -u root
CREATE DATABASE cpss;
CREATE USER 'cpss_user'@'localhost' IDENTIFIED BY '<PASSWORD>';
GRANT ALL PRIVILEGES ON cpss.* TO 'cpss_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3c. Import the database backup

```bash
mysql -u root cpss < /path/to/backup-YYYY-MM-DD_HH-MM.sql
```

> **Note:** The database uses Liquibase for schema management (DATABASECHANGELOG tables). The app will apply any pending migrations on startup, but importing a full backup is the fastest path.

The database has 9 application tables plus Liquibase's two bookkeeping tables:

- **Food core** — `food`, `nutrition`
- **Salads** — `salad`, `salad_food_ingredient`
- **Mixtures** — `mixture`, `mixture_ingredient`
- **Auth** — `users`, `password_reset_token`
- **Inherited scaffolding** — `company`
- **Liquibase** — `databasechangelog`, `databasechangeloglock`

All application tables carry the `BaseDb` columns (`id`, `extid`, `created_at`, `updated_at`,
`deleted_at`, `active`). None are large — the food catalog is ~166 rows — so a full dump/restore
is fast and a `mysqldump` backup is small.

> **On a fresh environment you may not need a backup at all.** Liquibase creates the schema on
> startup and seeds `company`/`users`; the food catalog, mixtures, and salads are then loaded
> from checked-in CSVs by `DataLoader` (see
> `../csv-load/liquibase-csv-loading-pattern.md`). Importing a backup is the right move only when
> you want to preserve user-created data from the source environment.

## Step 4: Create the Environment File

Create `/opt/cpss/.env`:

These are the variables `application.yml` actually reads (verified 2026-08-12):

```env
RDS_HOSTNAME=localhost
RDS_PORT=3306
RDS_DB_NAME=cpss
RDS_USERNAME=cpss_user
RDS_PASSWORD=<DATABASE_PASSWORD>
SPRING_PROFILES_ACTIVE=qa
PORT=8080

# Password-reset / username-reminder email (SMTP)
MAIL_HOST=<SMTP_HOST>
MAIL_PORT=587
MAIL_USERNAME=<SMTP_USERNAME>
MAIL_PASSWORD=<SMTP_APP_PASSWORD>
MAIL_FROM=<FROM_ADDRESS>

# Base URL used to build the emailed reset link — must be reachable by the user
FRONTEND_URL=https://<your-domain>

# Login rate limiting (LoginRateLimitFilter); defaults apply if unset
LOGIN_MAX_ATTEMPTS=8
LOGIN_LOCKOUT_MINUTES=15
OPENAI_API_KEY=<OPENAI_KEY>
```

Replace all `<PLACEHOLDER>` values with real credentials.

## Step 5: Deploy the Java Application

Copy the JAR to `/opt/cpss/cpss-server.jar` and also keep a copy in `/opt/cpss/releases/` for version tracking.

## Step 6: Create the Systemd Service for CPSS

Create `/etc/systemd/system/cpss.service`:

```ini
[Unit]
Description=CPSS Server Application
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/cpss
EnvironmentFile=/opt/cpss/.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/cpss/cpss-server.jar
Restart=always
RestartSec=10
StandardOutput=append:/var/log/cpss/app.log
StandardError=append:/var/log/cpss/error.log

[Install]
WantedBy=multi-user.target
```

Then enable and start:

```bash
systemctl daemon-reload
systemctl enable cpss
systemctl start cpss
```

The application is Spring Boot 3.5.7 on Java 21. It listens on port 8080 (`PORT`) and connects to
MySQL on localhost:3306. Liquibase applies pending migrations on startup, and `DataLoader` seeds
the food catalog from CSVs on the first boot against an empty database.

> CPSS has **no Spring AI, Anthropic, or OpenAI integration** — the source runbook's app did. No
> AI-related keys are needed in `.env`.

## Step 7: Deploy the Frontend

> **Decide which of the two frontend paths you are using.** `./gradlew buildDeployment` copies
> the built frontend into the jar's static resources, so the Spring Boot app serves the SPA
> itself and **this step is unnecessary** — point Nginx at the app and stop. Only unzip a
> separate frontend build if you deliberately want Nginx serving static files, which is faster
> for assets but means the frontend and backend version independently. The Nginx config in
> Step 8 assumes the separate-static-files layout; if you serve from the jar, proxy `/` to
> `localhost:8080` instead of using a `root`.

Unzip the frontend build into `/var/www/cpss/`:

```bash
unzip <frontend-build>.zip -d /var/www/cpss/
```

The resulting structure should be:

```
/var/www/cpss/
├── index.html
├── favicon.svg
├── favicon.jpg
├── vite.svg
└── assets/
    ├── *.js
    └── *.css
```

The frontend is a Vite-built SPA.

## Step 8: Configure Nginx

Create `/etc/nginx/sites-available/cpss`:

```nginx
server {
    listen 80;
    server_name <SERVER_IP_OR_DOMAIN>;

    root /var/www/cpss;
    index index.html;

    client_max_body_size 10M;

    # SPA fallback routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API requests to Java backend
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Swagger UI
    location /swagger-ui/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # API docs
    location /v3/api-docs {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

Replace `<SERVER_IP_OR_DOMAIN>` with the new server's IP or domain name.

Enable the site and restart:

```bash
ln -s /etc/nginx/sites-available/cpss /etc/nginx/sites-enabled/cpss
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl restart nginx
systemctl enable nginx
```

Ensure gzip is enabled in `/etc/nginx/nginx.conf` (Ubuntu default usually has it on).

## Step 9: Deploy n8n (Workflow Automation)

Run n8n as a Docker container:

```bash
docker run -d \
  --name n8n \
  --restart unless-stopped \
  -p 5678:5678 \
  -v /opt/n8n:/home/node/.n8n \
  -e N8N_SECURE_COOKIE=false \
  docker.n8n.io/n8nio/n8n:latest
```

n8n will be accessible on port 5678. On first launch it will prompt for owner account setup.

**What CPSS uses n8n for.** The database-rebuild webhook: `GET /webhook/clear-cpss-db` truncates
the application tables so Liquibase and `DataLoader` repopulate from scratch on the next restart.
This is the documented way to "rebuild the database" (`.claude/CLAUDE.md`) and the workaround for
`spring.liquibase.drop-first: false`, which means edits to an already-applied changeset never
re-run. **The workflow itself is not provisioned by these steps** — a fresh n8n install is empty,
so the webhook must be recreated or imported before that path works.

⚠️ **Do not expose port 5678 publicly.** `N8N_SECURE_COOKIE=false` disables cookie security for
convenience on a local/QA box, and this webhook destroys data with an unauthenticated `GET`.
Keep it firewalled to localhost or bind it behind authentication.

## Step 10: Verify All Services

```bash
systemctl status mysql
systemctl status nginx
systemctl status cpss
docker ps   # should show n8n container running
```

Check ports are listening:

```bash
ss -tlnp | grep -E ':(80|3306|5678|8080)\s'
```

Expected:

| Port | Service |
|------|---------|
| 80   | Nginx |
| 3306 | MySQL |
| 5678 | n8n (docker-proxy) |
| 8080 | CPSS Java app |

Test the application:

```bash
curl -s http://localhost/api/auth/login -o /dev/null -w "%{http_code}"
curl -s http://localhost/ | head -5
```

---

## Architecture Summary

```
Client (HTTP :80)
  │
  ▼
Nginx (reverse proxy)
  ├─ Static files ──→ /var/www/cpss/ (SPA frontend)
  └─ /api/*, /swagger-ui/*, /v3/api-docs ──→ localhost:8080
                                                │
                                          Spring Boot App
                                          (cpss-server.jar)
                                                │
                                          MySQL (localhost:3306)
                                          Database: cpss

n8n (Docker, :5678) ── standalone workflow automation
```

---

## Notes & Security Considerations

1. **No HTTPS configured** on the source deployment. For production, set up TLS with Let's Encrypt (`certbot --nginx`) or provide certificates.
2. **MySQL is bound to 0.0.0.0** — consider restricting to 127.0.0.1 if only local access is needed, or use a firewall.
3. **The Java app runs as root** — consider creating a dedicated service user for better security.
4. **Log rotation** is not explicitly configured for `/var/log/cpss/` — the app.log can grow large (~100MB observed). Set up logrotate.
5. **Database backups** appear to be manual. Consider automating with a cron job:
   ```bash
   # Example: daily backup at 4 AM
   0 4 * * * mysqldump -u cpss_user -p'<PASSWORD>' cpss > /opt/cpss/db_backup/backup-$(date +\%Y-\%m-\%d_\%H-\%M).sql
   ```
6. **Spring profile** is set to `qa`. Change `SPRING_PROFILES_ACTIVE` in `.env` as appropriate for the target environment.
7. Frontend and backend versions should match (currently both at version ~1.21/0.1.21).
