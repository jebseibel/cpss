# Viro Deployment Replication Instructions

These instructions are written for a Claude instance to replicate the Viro deployment on a fresh Ubuntu server.

---

## Prerequisites

- Fresh Ubuntu server (22.04+ recommended)
- Root/sudo access
- The following artifacts copied from the source machine:
  - `/opt/viro/viro-server.jar` (current: v0.1.21-SNAPSHOT, ~176MB)
  - A frontend build zip (current: `frontend-1.21.0-build.zip`)
  - A database backup SQL file (latest from `/opt/viro/db_backup/`)
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
mkdir -p /opt/viro/releases
mkdir -p /opt/viro/db_backup
mkdir -p /var/www/viro
mkdir -p /var/log/viro
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
CREATE DATABASE viro;
CREATE USER 'viro_user'@'localhost' IDENTIFIED BY '<PASSWORD>';
GRANT ALL PRIVILEGES ON viro.* TO 'viro_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3c. Import the database backup

```bash
mysql -u root viro < /path/to/backup-YYYY-MM-DD_HH-MM.sql
```

> **Note:** The database uses Liquibase for schema management (DATABASECHANGELOG tables). The app will apply any pending migrations on startup, but importing a full backup is the fastest path.

The database has ~47 tables including:
- `company`, `users`, `customers_transactions`
- `crs_approved`, `crs_change`, `crs_mistake`, `crs_pending`
- `facility_output` (largest table)
- Tracking system tables: `ts_ercot`, `ts_mrets`, `ts_wregis`, `ts_nar`, `ts_ncrets`, `ts_mirecs` (and corresponding `load_*` and `ret_*` tables)
- `doc_incoming`, `document_metadata`, `stored_document`
- Various type/enum tables: `import_status_type`, `renewable_type`, `region_nec_type`, `tracking_system_type`, `tracking_attestation_status`

## Step 4: Create the Environment File

Create `/opt/viro/.env`:

```env
RDS_HOSTNAME=localhost
RDS_PORT=3306
RDS_DB_NAME=viro
RDS_USERNAME=viro_user
RDS_PASSWORD=<DATABASE_PASSWORD>
SPRING_PROFILES_ACTIVE=qa
SERVER_PORT=8080
ANTHROPIC_API_KEY=<ANTHROPIC_KEY>
OPENAI_API_KEY=<OPENAI_KEY>
```

Replace all `<PLACEHOLDER>` values with real credentials.

## Step 5: Deploy the Java Application

Copy the JAR to `/opt/viro/viro-server.jar` and also keep a copy in `/opt/viro/releases/` for version tracking.

## Step 6: Create the Systemd Service for Viro

Create `/etc/systemd/system/viro.service`:

```ini
[Unit]
Description=Viro Server Application
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/viro
EnvironmentFile=/opt/viro/.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/viro/viro-server.jar
Restart=always
RestartSec=10
StandardOutput=append:/var/log/viro/app.log
StandardError=append:/var/log/viro/error.log

[Install]
WantedBy=multi-user.target
```

Then enable and start:

```bash
systemctl daemon-reload
systemctl enable viro
systemctl start viro
```

The application is Spring Boot 3.5.5 on Java 21. It listens on port 8080 and connects to MySQL on localhost:3306. It includes Spring AI integrations with Anthropic and OpenAI.

## Step 7: Deploy the Frontend

Unzip the frontend build into `/var/www/viro/`:

```bash
unzip frontend-1.21.0-build.zip -d /var/www/viro/
```

The resulting structure should be:

```
/var/www/viro/
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

Create `/etc/nginx/sites-available/viro`:

```nginx
server {
    listen 80;
    server_name <SERVER_IP_OR_DOMAIN>;

    root /var/www/viro;
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
ln -s /etc/nginx/sites-available/viro /etc/nginx/sites-enabled/viro
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

## Step 10: Verify All Services

```bash
systemctl status mysql
systemctl status nginx
systemctl status viro
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
| 8080 | Viro Java app |

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
  ├─ Static files ──→ /var/www/viro/ (SPA frontend)
  └─ /api/*, /swagger-ui/*, /v3/api-docs ──→ localhost:8080
                                                │
                                          Spring Boot App
                                          (viro-server.jar)
                                                │
                                          MySQL (localhost:3306)
                                          Database: viro

n8n (Docker, :5678) ── standalone workflow automation
```

---

## Notes & Security Considerations

1. **No HTTPS configured** on the source deployment. For production, set up TLS with Let's Encrypt (`certbot --nginx`) or provide certificates.
2. **MySQL is bound to 0.0.0.0** — consider restricting to 127.0.0.1 if only local access is needed, or use a firewall.
3. **The Java app runs as root** — consider creating a dedicated service user for better security.
4. **Log rotation** is not explicitly configured for `/var/log/viro/` — the app.log can grow large (~100MB observed). Set up logrotate.
5. **Database backups** appear to be manual. Consider automating with a cron job:
   ```bash
   # Example: daily backup at 4 AM
   0 4 * * * mysqldump -u viro_user -p'<PASSWORD>' viro > /opt/viro/db_backup/backup-$(date +\%Y-\%m-\%d_\%H-\%M).sql
   ```
6. **Spring profile** is set to `qa`. Change `SPRING_PROFILES_ACTIVE` in `.env` as appropriate for the target environment.
7. Frontend and backend versions should match (currently both at version ~1.21/0.1.21).
