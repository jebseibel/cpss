# cpss Deployment Guide

## Quick Commands

a. Update the build gradle version number (line 9)
b. Make sure SecurityOptions are closed

```bash
# 1. Tail the logs
ssh cpss "tail -f /var/log/cpss/app.log"

# 2. Stop the service
ssh cpss "systemctl stop cpss"

# 3. Backup database
ssh cpss 'source /opt/cpss/.env && mysqldump --no-tablespaces -u cpss_user -p"$RDS_PASSWORD" cpss > /opt/cpss/db_backup/backup-$(date +%Y-%m-%d_%H-%M).sql'

# 4. Build and deploy backend
cd /home/jeb/projects/personal/cpss && ./gradlew clean build -x test && scp build/libs/cpss-*-SNAPSHOT.jar cpss:/opt/cpss/cpss.jar

# 5. Build and deploy frontend
cd /home/jeb/projects/personal/cpss/frontend && npx vite build && scp -r dist/* cpss:/var/www/cpss/

# 6. Drop and rebuild database
ssh cpss 'source /opt/cpss/.env && mysql -u cpss_user -p"$RDS_PASSWORD" -e "DROP DATABASE cpss; CREATE DATABASE cpss;"'

# 7. Restart service
ssh cpss "systemctl restart cpss"
```

---

## Step-by-Step Reference

### Prerequisites
- SSH access configured (`cpss` alias in `~/.ssh/config`)
- Local project at `/home/jeb/projects/personal/cpss`

### Update Version Number
Before deploying, update the version in `build.gradle` (line 9):
```
version = '0.0.3-SNAPSHOT'
```

---

### Deploy Frontend

1. Build frontend
```bash
cd /home/jeb/projects/personal/cpss/frontend
npx vite build
```

2. Transfer to VPS
```bash
scp -r dist/* cpss:/var/www/cpss/
```

No restart needed for frontend-only changes.

---

### Deploy Backend

1. Stop application (optional, for database changes)
```bash
ssh cpss "systemctl stop cpss"
```

2. Build JAR
```bash
cd /home/jeb/projects/personal/cpss
./gradlew clean build -x test
```

3. Transfer to VPS
```bash
scp build/libs/cpss-*-SNAPSHOT.jar cpss:/opt/cpss/cpss.jar
```

4. Restart application
```bash
ssh cpss "systemctl restart cpss"
```

---

### Verify Deployment

Check application started:
```bash
ssh cpss "systemctl status cpss"
```

Check health endpoint:
```bash
curl http://72.61.75.82/api/actuator/health
```

View startup logs (wait for "Started CpssApplication"):
```bash
ssh cpss "tail -f /var/log/cpss/app.log"
```

---

### Database Operations

**Backup before changes:**
```bash
ssh cpss 'source /opt/cpss/.env && mysqldump --no-tablespaces -u cpss_user -p"$RDS_PASSWORD" cpss > /opt/cpss/db_backup/backup-$(date +%Y-%m-%d_%H-%M).sql'
```

**Reset database (Liquibase checksum errors):**
```bash
ssh cpss 'source /opt/cpss/.env && mysql -u cpss_user -p"$RDS_PASSWORD" -e "DROP DATABASE cpss; CREATE DATABASE cpss;" && systemctl restart cpss'
```

**List backups:**
```bash
ssh cpss "ls -lh /opt/cpss/db_backup/"
```

**Download latest backup:**
```bash
scp cpss:/opt/cpss/db_backup/$(ssh cpss "ls -t /opt/cpss/db_backup/ | head -1") .
```

---

### Rollback

Create backup before deploying:
```bash
ssh cpss "cp /opt/cpss/cpss.jar /opt/cpss/cpss.jar.bak"
```

Restore if something goes wrong:
```bash
ssh cpss "mv /opt/cpss/cpss.jar.bak /opt/cpss/cpss.jar && systemctl restart cpss"
```
