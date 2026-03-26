#!/bin/bash
set -euo pipefail

# =============================================================================
# Example Deployment Setup Script
# Run as root on a fresh Ubuntu 22.04+ server
# =============================================================================

# --- Configuration (update these before running) ---
DB_NAME="example"
DB_USER="example_user"
DB_PASSWORD="${DB_PASSWORD:?Set DB_PASSWORD environment variable}"
SERVER_NAME="${SERVER_NAME:?Set SERVER_NAME environment variable (IP or domain)}"
ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY:-}"
OPENAI_API_KEY="${OPENAI_API_KEY:-}"
SPRING_PROFILE="${SPRING_PROFILE:-qa}"

JAR_PATH="${JAR_PATH:-/opt/example/example-server.jar}"
FRONTEND_ZIP="${FRONTEND_ZIP:-/opt/example/frontend-build.zip}"
DB_BACKUP_SQL="${DB_BACKUP_SQL:-}"

# --- Step 1: Install System Packages ---
echo ">>> Step 1: Installing system packages..."
apt update && apt upgrade -y
apt install -y nginx mysql-server openjdk-21-jre-headless docker.io unzip curl
systemctl enable docker && systemctl start docker

# --- Step 2: Create Directory Structure ---
echo ">>> Step 2: Creating directories..."
mkdir -p /opt/example/releases
mkdir -p /opt/example/db_backup
mkdir -p /var/www/example
mkdir -p /var/log/example
mkdir -p /opt/n8n

# --- Step 3: Configure MySQL ---
echo ">>> Step 3: Configuring MySQL..."
cat > /etc/mysql/mysql.conf.d/zzz-example.cnf <<'MYSQLCNF'
[mysqld]
bind-address = 127.0.0.1
mysqlx-bind-address = 127.0.0.1
key_buffer_size = 16M
max_binlog_size = 100M
MYSQLCNF

systemctl restart mysql
systemctl enable mysql

mysql -u root <<EOSQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME};
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
EOSQL

if [ -n "$DB_BACKUP_SQL" ] && [ -f "$DB_BACKUP_SQL" ]; then
  echo ">>> Importing database backup: $DB_BACKUP_SQL"
  mysql -u root "$DB_NAME" < "$DB_BACKUP_SQL"
else
  echo ">>> Skipping DB import (no DB_BACKUP_SQL provided or file not found)"
fi

# --- Step 4: Create Environment File ---
echo ">>> Step 4: Creating /opt/example/.env..."
cat > /opt/example/.env <<ENVFILE
RDS_HOSTNAME=localhost
RDS_PORT=3306
RDS_DB_NAME=${DB_NAME}
RDS_USERNAME=${DB_USER}
RDS_PASSWORD=${DB_PASSWORD}
SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}
SERVER_PORT=8080
ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
OPENAI_API_KEY=${OPENAI_API_KEY}
ENVFILE
chmod 600 /opt/example/.env

# --- Step 5: Deploy JAR ---
echo ">>> Step 5: Deploying Java application..."
if [ -f "$JAR_PATH" ]; then
  cp "$JAR_PATH" "/opt/example/releases/example-server-$(date +%Y%m%d).jar"
  echo "JAR ready at $JAR_PATH"
else
  echo "WARNING: JAR not found at $JAR_PATH — copy it there before starting the service"
fi

# --- Step 6: Create Systemd Service ---
echo ">>> Step 6: Creating example systemd service..."
cat > /etc/systemd/system/example.service <<'UNIT'
[Unit]
Description=Example Server Application
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/example
EnvironmentFile=/opt/example/.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/example/example-server.jar
Restart=always
RestartSec=10
StandardOutput=append:/var/log/example/app.log
StandardError=append:/var/log/example/error.log

[Install]
WantedBy=multi-user.target
UNIT

systemctl daemon-reload
systemctl enable example

# --- Step 7: Deploy Frontend ---
echo ">>> Step 7: Deploying frontend..."
if [ -f "$FRONTEND_ZIP" ]; then
  rm -rf /var/www/example/*
  unzip -o "$FRONTEND_ZIP" -d /var/www/example/
else
  echo "WARNING: Frontend zip not found at $FRONTEND_ZIP — copy it and unzip to /var/www/example/"
fi

# --- Step 8: Configure Nginx ---
echo ">>> Step 8: Configuring Nginx..."
cat > /etc/nginx/sites-available/example <<NGINX
server {
    listen 80;
    server_name ${SERVER_NAME};

    root /var/www/example;
    index index.html;

    client_max_body_size 10M;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /swagger-ui/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }

    location /v3/api-docs {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}
NGINX

ln -sf /etc/nginx/sites-available/example /etc/nginx/sites-enabled/example
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl restart nginx
systemctl enable nginx

# --- Step 9: Deploy n8n ---
echo ">>> Step 9: Starting n8n container..."
docker rm -f n8n 2>/dev/null || true
docker run -d \
  --name n8n \
  --restart unless-stopped \
  -p 5678:5678 \
  -v /opt/n8n:/home/node/.n8n \
  -e N8N_SECURE_COOKIE=false \
  docker.n8n.io/n8nio/n8n:latest

# --- Step 10: Start Example & Verify ---
echo ">>> Step 10: Starting Example and verifying..."
if [ -f "$JAR_PATH" ]; then
  systemctl start example
fi

echo ""
echo "=== Service Status ==="
systemctl is-active mysql   && echo "MySQL:  OK" || echo "MySQL:  NOT RUNNING"
systemctl is-active nginx   && echo "Nginx:  OK" || echo "Nginx:  NOT RUNNING"
systemctl is-active example    && echo "Example:   OK" || echo "Example:   NOT RUNNING (JAR may be missing)"
docker ps --format '{{.Names}}' | grep -q n8n && echo "n8n:    OK" || echo "n8n:    NOT RUNNING"

echo ""
echo "=== Listening Ports ==="
ss -tlnp | grep -E ':(80|3306|5678|8080)\s' || true

echo ""
echo ">>> Setup complete."
echo ">>> Artifacts needed if not already provided:"
echo "    - JAR:          $JAR_PATH"
echo "    - Frontend zip: $FRONTEND_ZIP"
echo "    - DB backup:    set DB_BACKUP_SQL and re-run, or import manually"
