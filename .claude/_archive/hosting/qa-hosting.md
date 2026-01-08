# cpss Environment Operations Guide

## Purpose

This guide covers **day-to-day operations** for the running cpss environment: monitoring, troubleshooting, and maintenance. For initial setup, see `cpss-setup.md`. For deploying code changes, see `cpss-deployment.md`.

---

## Current cpss Environment

| Item | Value |
|------|-------|
| **IP Address** | 72.61.75.82 |
| **Provider** | Hostinger |
| **OS** | Ubuntu 24.04.3 LTS |
| **SSH** | `ssh cpss` (or `ssh root@72.61.75.82`) |

---

## Architecture

```
Client Browser
      |
      v
+-------------------------+
|  Nginx (port 80)        |
|  - Serves React build   |
|  - Proxies /api -> 8080 |
+------------+------------+
             |
             v
+-------------------------+
|  Spring Boot (:8080)    |
|  - REST API             |
+------------+------------+
             |
             v
+-------------------------+
|  MySQL (:3306)          |
|  - Local only           |
+-------------------------+
```

---

## Quick Reference

### Access URLs

| Service | URL |
|---------|-----|
| Frontend | http://72.61.75.82/ |
| API | http://72.61.75.82/api/ |
| Swagger | http://72.61.75.82/swagger-ui.html |
| Health Check | http://72.61.75.82/api/actuator/health |

### File Locations

| File | Path |
|------|------|
| JAR | `/opt/cpss/cpss.jar` |
| Environment | `/opt/cpss/.env` |
| Frontend | `/var/www/cpss/` |
| Systemd Service | `/etc/systemd/system/cpss.service` |
| Nginx Config | `/etc/nginx/sites-available/cpss` |
| App Log | `/var/log/cpss/app.log` |
| Error Log | `/var/log/cpss/error.log` |

### Database

| Item | Value |
|------|-------|
| Database | `cpss` |
| User | `cpss_user` |
| Password | See `/opt/cpss/.env` |
| Host | `localhost:3306` |

---

## Application Management

### Start / Stop / Restart

```bash
# SSH to VPS first
ssh cpss

# Commands
systemctl start cpss
systemctl stop cpss
systemctl restart cpss

# Check status
systemctl status cpss
```

### View Logs

```bash
# Live application log
tail -f /var/log/cpss/app.log

# Live error log
tail -f /var/log/cpss/error.log

# Last 100 lines
tail -100 /var/log/cpss/app.log

# Search logs
grep "ERROR" /var/log/cpss/app.log
```

### Check Health

```bash
# From VPS
curl http://localhost:8080/actuator/health

# From anywhere
curl http://72.61.75.82/api/actuator/health
```

---

## Database Operations

### Connect to MySQL

```bash
mysql -u cpss_user -p cpss
# Password in /opt/cpss/.env
```

### Useful Queries

```sql
-- Check table counts
SELECT 'food' as tbl, COUNT(*) as cnt FROM food
UNION ALL SELECT 'mixture', COUNT(*) FROM mixture
UNION ALL SELECT 'salad', COUNT(*) FROM salad;

-- Check recent foods
SELECT name, category, created_at FROM food ORDER BY created_at DESC LIMIT 10;
```

### Backup Database

```bash
# Create backup
mysqldump -u cpss_user -p cpss > /opt/cpss/backup-$(date +%Y%m%d).sql

# Restore from backup
mysql -u cpss_user -p cpss < backup-20241208.sql
```

### Clear Database (Liquibase Checksum Errors)

When Liquibase changelogs are modified after being applied, you'll get checksum validation errors. Fix by clearing and recreating the database:

```bash
# Get password from env file
source /opt/cpss/.env

# Option 1: Drop and recreate entire database (cleanest)
mysql -u cpss_user -p"$DB_PASSWORD" -e "DROP DATABASE cpss; CREATE DATABASE cpss;"
systemctl restart cpss

# Option 2: Clear only Liquibase tracking tables (keeps data structure, risky)
mysql -u cpss_user -p"$DB_PASSWORD" cpss -e "DELETE FROM DATABASECHANGELOG; DELETE FROM DATABASECHANGELOGLOCK;"
systemctl restart cpss
```

Note: If entering password manually, use single quotes around it (e.g., `-p'password'`) if it contains `!`.

---

## Nginx Operations

```bash
# Test configuration
nginx -t

# Reload (after config change)
systemctl reload nginx

# View access log
tail -f /var/log/nginx/access.log

# View error log
tail -f /var/log/nginx/error.log
```

---

## Troubleshooting

### Application Won't Start

```bash
# Check status and recent logs
systemctl status cpss
tail -50 /var/log/cpss/error.log

# Check if port is in use
lsof -i :8080

# Check Java version
java -version

# Test database connection
mysql -u cpss_user -p -e "SELECT 1"
```

### API Returns 502 Bad Gateway

Backend is not running or not responding:

```bash
# Check if backend is running
systemctl status cpss

# Check if backend responds
curl http://localhost:8080/actuator/health

# Restart if needed
systemctl restart cpss
```

### Frontend Shows Blank Page

```bash
# Check if files exist
ls -la /var/www/cpss/

# Check nginx config
nginx -t

# Check nginx error log
tail -20 /var/log/nginx/error.log
```

### Database Connection Issues

```bash
# Check MySQL is running
systemctl status mysql

# Test connection
mysql -u cpss_user -p cpss -e "SHOW TABLES"

# Check credentials in env file
cat /opt/cpss/.env
```

### Out of Memory

```bash
# Check memory usage
free -h

# Check what's using memory
ps aux --sort=-%mem | head -10

# Restart application (clears memory)
systemctl restart cpss
```

### Disk Space Issues

```bash
# Check disk usage
df -h

# Find large files
du -sh /var/log/cpss/*
du -sh /opt/cpss/*

# Truncate large log files (if needed)
truncate -s 0 /var/log/cpss/app.log
```

---

## Monitoring

### Quick Health Check Script

```bash
#!/bin/bash
echo "=== CPSS cpss Status ==="
echo ""
echo "Application:"
systemctl is-active cpss && echo "  Status: RUNNING" || echo "  Status: DOWN"
curl -s http://localhost:8080/actuator/health | grep -q "UP" && echo "  Health: UP" || echo "  Health: DOWN"
echo ""
echo "MySQL:"
systemctl is-active mysql && echo "  Status: RUNNING" || echo "  Status: DOWN"
echo ""
echo "Nginx:"
systemctl is-active nginx && echo "  Status: RUNNING" || echo "  Status: DOWN"
echo ""
echo "Memory:"
free -h | grep Mem | awk '{print "  Used: " $3 " / " $2}'
echo ""
echo "Disk:"
df -h / | tail -1 | awk '{print "  Used: " $3 " / " $2 " (" $5 ")"}'
```

### Memory Usage (Typical)

| Process | RAM |
|---------|-----|
| Spring Boot | ~400-500 MB |
| MySQL | ~500-600 MB |
| Nginx | ~50 MB |
| OS | ~200 MB |
| **Total** | **~1.2-1.4 GB** |

---

## Maintenance

### Clear Old Logs

```bash
# Truncate logs (keeps file, clears content)
truncate -s 0 /var/log/cpss/app.log
truncate -s 0 /var/log/cpss/error.log

# Or rotate manually
mv /var/log/cpss/app.log /var/log/cpss/app.log.old
systemctl restart cpss
```

### Update System Packages

```bash
apt update && apt upgrade -y
# Reboot if kernel updated
reboot
```

### Renew SSL Certificate

If SSL is configured:

```bash
certbot renew
systemctl reload nginx
```

---

## Emergency Procedures

### Full Restart (Nuclear Option)

```bash
systemctl stop cpss
systemctl restart mysql
systemctl restart nginx
systemctl start cpss
```

### Rollback Deployment

```bash
# If you kept backup
mv /opt/cpss/cpss.jar.bak /opt/cpss/cpss.jar
systemctl restart cpss
```

### Check What Changed Recently

```bash
# Recent deployments
ls -la /opt/cpss/
ls -la /var/www/cpss/

# Recent log entries
tail -200 /var/log/cpss/app.log | head -50
```

---

## Cost

| Item | Monthly |
|------|---------|
| Hostinger KVM 1 (4GB) | $4.99 |
| Domain (optional) | ~$1 |
| **Total** | **~$5-6/mo** |
