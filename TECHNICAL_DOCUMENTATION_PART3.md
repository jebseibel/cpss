# CPSS Technical Documentation - Part 3

## Development Setup

### Prerequisites

**Required Software:**
- Java 21 or higher (OpenJDK or Oracle JDK)
- Node.js 18+ and npm 9+
- MySQL 8.0+
- Git
- IDE (IntelliJ IDEA recommended for Java, VS Code for frontend)

**Optional Tools:**
- Postman (API testing) - collection included in `documents/postman/`
- Docker (for MySQL containerization)
- n8n (for database reset automation)

### Initial Setup Steps

#### 1. Clone Repository
```bash
git clone <repository-url>
cd cpss
```

#### 2. Database Setup

**Option A: Local MySQL Server**
```sql
-- Create database
CREATE DATABASE cpss CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user (optional)
CREATE USER 'cpss_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON cpss.* TO 'cpss_user'@'localhost';
FLUSH PRIVILEGES;
```

**Option B: Docker MySQL**
```bash
docker run --name cpss-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -e MYSQL_DATABASE=cpss \
  -e MYSQL_USER=cpss_user \
  -e MYSQL_PASSWORD=your_password \
  -p 3306:3306 \
  -d mysql:8.0
```

#### 3. Environment Configuration

Create `.env` file in project root:
```bash
# Database
RDS_HOSTNAME=localhost
RDS_PORT=3306
RDS_DB_NAME=cpss
RDS_USERNAME=cpss_user
RDS_PASSWORD=your_password

# Email (Gmail)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password
MAIL_FROM=noreply@cpss.com

# Frontend
FRONTEND_URL=http://localhost:5173
```

**Note:** `.env` file is git-ignored for security. Never commit credentials.

#### 4. Backend Setup

**Install dependencies and run Liquibase migrations:**
```bash
# Clean and build
./gradlew clean build

# Run database migrations
./gradlew update

# Run application
./gradlew bootRun
```

Backend will start on `http://localhost:8080`

**Verify backend:**
```bash
curl http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```

#### 5. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start dev server
npm run dev
```

Frontend will start on `http://localhost:5173` with hot reload enabled.

**Vite Proxy Configuration:**
The dev server proxies `/api` requests to `http://localhost:8080` (backend).

### IDE Configuration

#### IntelliJ IDEA (Backend)

1. **Open Project:** File → Open → Select `cpss` directory
2. **SDK:** File → Project Structure → Project SDK → Java 21
3. **Gradle:** IntelliJ auto-detects `build.gradle`
4. **Lombok Plugin:** Settings → Plugins → Install "Lombok"
5. **Enable Annotation Processing:** Settings → Build → Compiler → Annotation Processors → Enable
6. **Run Configuration:**
   - Main class: `com.seibel.cpss.CpssApplication`
   - VM options: `-Dspring.profiles.active=dev`
   - Environment variables: Load from `.env`

#### VS Code (Frontend)

**Recommended Extensions:**
- ESLint
- Prettier
- Tailwind CSS IntelliSense
- TypeScript Vue Plugin (Volar)

**Settings (.vscode/settings.json):**
```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  }
}
```

### Running in Development Mode

#### Concurrent Development (Both Frontend & Backend)

**Terminal 1 (Backend):**
```bash
./gradlew bootRun
```

**Terminal 2 (Frontend):**
```bash
cd frontend
npm run dev
```

**Access Application:**
- Frontend: `http://localhost:5173` (with hot reload)
- Backend API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

#### Frontend-Only Development

If backend is already running (or deployed):
```bash
cd frontend
VITE_API_URL=http://localhost:8080/api npm run dev
```

### Database Management

#### Run Liquibase Migrations
```bash
./gradlew update
```

#### Rollback Last Migration
```bash
./gradlew rollbackCount -PliquibaseCommandValue=1
```

#### Generate Changelog Diff
```bash
./gradlew diffChangeLog
```

#### Reset Database (Development)

**Using n8n Webhook (if configured):**
```bash
curl http://localhost:5678/webhook/clear-cpss-db
```

**Manual Reset:**
```sql
DROP DATABASE cpss;
CREATE DATABASE cpss CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Then run `./gradlew update` to re-apply migrations.

---

## Build & Deployment Process

### Local Build

#### Build Backend Only
```bash
./gradlew clean build

# Output: build/libs/cpss-0.0.2-SNAPSHOT.jar
```

#### Build Frontend Only
```bash
cd frontend
npm run build

# Output: frontend/dist/
```

#### Build Full Deployment Package
```bash
# This builds frontend and bundles it into backend JAR
./gradlew buildDeployment

# Output: build/libs/cpss-0.0.2-SNAPSHOT.jar
# Contains: Backend + Frontend static files
```

**What `buildDeployment` does:**
1. Runs `npmInstall` (install frontend dependencies)
2. Runs `npmBuild` (builds frontend to `frontend/dist/`)
3. Runs `cleanStatic` (clears old static files)
4. Runs `copyFrontend` (copies `dist/` to `src/main/resources/static/`)
5. Runs `build` (builds Spring Boot JAR with embedded frontend)

### Deployment to AWS Elastic Beanstalk

#### Prerequisites
- AWS Account
- AWS CLI installed and configured
- EB CLI installed (`pip install awsebcli`)
- RDS MySQL instance created

#### Initial Setup

**1. Initialize Elastic Beanstalk:**
```bash
eb init

# Prompts:
# - Select region (e.g., us-east-1)
# - Select application name (e.g., cpss)
# - Select platform (Java 21)
# - Setup SSH (optional)
```

**2. Create Environment:**
```bash
eb create cpss-prod

# This creates:
# - Application Load Balancer
# - EC2 instance(s)
# - Security groups
# - CloudWatch logs
```

**3. Configure Environment Variables:**
```bash
eb setenv \
  RDS_HOSTNAME=<rds-endpoint> \
  RDS_PORT=3306 \
  RDS_DB_NAME=cpss \
  RDS_USERNAME=admin \
  RDS_PASSWORD=<password> \
  MAIL_HOST=smtp.gmail.com \
  MAIL_PORT=587 \
  MAIL_USERNAME=<gmail> \
  MAIL_PASSWORD=<app-password> \
  MAIL_FROM=noreply@cpss.com \
  FRONTEND_URL=<eb-url> \
  cors.allowed.origins=<eb-url>
```

**4. Deploy Application:**
```bash
# Build deployment package
./gradlew buildDeployment

# Deploy to EB
eb deploy

# Monitor deployment
eb logs --stream
```

**5. Verify Deployment:**
```bash
# Get environment URL
eb status

# Open in browser
eb open
```

#### Continuous Deployment

**Manual Deployment:**
```bash
./gradlew buildDeployment && eb deploy
```

**Automated CI/CD (GitHub Actions Example):**
```yaml
name: Deploy to EB

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: 21

      - name: Build deployment package
        run: ./gradlew buildDeployment

      - name: Deploy to EB
        uses: einaregilsson/beanstalk-deploy@v21
        with:
          aws_access_key: ${{ secrets.AWS_ACCESS_KEY }}
          aws_secret_key: ${{ secrets.AWS_SECRET_KEY }}
          application_name: cpss
          environment_name: cpss-prod
          version_label: ${{ github.sha }}
          region: us-east-1
          deployment_package: build/libs/cpss-0.0.2-SNAPSHOT.jar
```

### Deployment Checklist

**Pre-Deployment:**
- [ ] All tests passing (`./gradlew test`)
- [ ] Frontend builds without errors (`npm run build`)
- [ ] Database migrations tested locally
- [ ] Environment variables configured
- [ ] RDS security group allows EB access
- [ ] Gmail app password created

**Post-Deployment:**
- [ ] Health check passing (`eb health`)
- [ ] Application accessible via EB URL
- [ ] Login functionality working
- [ ] Database migrations applied (check logs)
- [ ] Email sending working (test password reset)
- [ ] API endpoints responding correctly

### Rollback Procedure

**Rollback to Previous Version:**
```bash
# List recent deployments
eb appversion

# Deploy specific version
eb deploy --version <version-label>
```

**Emergency Rollback:**
1. Go to AWS Console → Elastic Beanstalk
2. Select environment
3. Click "Actions" → "Deploy a different version"
4. Select previous version
5. Click "Deploy"

---

## Configuration Management

### Application Configuration

**File:** `src/main/resources/application.yml`

```yaml
server:
  port: ${PORT:8080}  # Default 8080, override with PORT env var

spring:
  datasource:
    url: jdbc:mysql://${RDS_HOSTNAME}:${RDS_PORT}/${RDS_DB_NAME}
    username: ${RDS_USERNAME}
    password: ${RDS_PASSWORD}
    hikari:
      connection-timeout: 30000  # 30 seconds
      initialization-fail-timeout: 0  # Fail fast

  jpa:
    show-sql: false  # Set true for SQL logging in dev
    hibernate:
      ddl-auto: none  # Schema managed by Liquibase

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    drop-first: false  # NEVER set true in production

  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp:
        auth: true
        starttls.enable: true

app:
  email:
    from: ${MAIL_FROM:noreply@cpss.com}
  frontend:
    url: ${FRONTEND_URL:http://localhost:5173}

logging:
  level:
    com.seibel.cpss: INFO
    org.springframework.security: ERROR
```

**Configuration Hierarchy:**
1. Default values in `application.yml`
2. Environment variables (override defaults)
3. System properties (override env vars)

### Frontend Configuration

**File:** `frontend/vite.config.ts`

```typescript
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
```

**Environment Variables (.env files):**
```
# Development (.env.development)
VITE_API_URL=/api

# Production (.env.production)
VITE_API_URL=/api
```

**Usage in Code:**
```typescript
const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';
```

### Security Configuration

**JWT Settings (Backend):**
```yaml
jwt:
  secret: ${JWT_SECRET:MySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong1234567890}
  expiration: ${JWT_EXPIRATION:86400000}  # 24 hours in ms
```

**CORS Settings:**
```yaml
cors:
  allowed:
    origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}
```

**Production Security Recommendations:**
1. **JWT Secret:** Use cryptographically random 256+ bit secret
2. **CORS Origins:** Restrict to production domain only
3. **HTTPS:** Enable SSL/TLS in production (via ALB or CloudFront)
4. **Database Credentials:** Use AWS Secrets Manager instead of env vars

---

## Key Features Deep Dive

### Feature 1: Real-Time Nutritional Calculation

**How It Works:**

1. **Data Model:** All nutrition data stored per 100g in database
2. **User Input:** User specifies grams for each ingredient
3. **Scaling Formula:**
   ```
   actualValue = (valuePer100g * actualGrams) / 100
   ```
4. **Aggregation:** Sum all scaled values across ingredients
5. **Calories:** Calculated from macros: `(carbs × 4) + (protein × 4) + (fat × 9)`

**Example Calculation:**

```
Salad Ingredients:
- Spinach 100g: 4g carbs, 3g protein, 0g fat per 100g
- Chicken 150g: 0g carbs, 31g protein, 4g fat per 100g

Scaled Nutrition:
Spinach:
  carbs = (4 × 100) / 100 = 4g
  protein = (3 × 100) / 100 = 3g
  fat = (0 × 100) / 100 = 0g

Chicken:
  carbs = (0 × 150) / 100 = 0g
  protein = (31 × 150) / 100 = 46.5g
  fat = (4 × 150) / 100 = 6g

Totals:
  carbs = 4 + 0 = 4g
  protein = 3 + 46.5 = 49.5g
  fat = 0 + 6 = 6g
  calories = (4 × 4) + (49.5 × 4) + (6 × 9) = 16 + 198 + 54 = 268 cal
```

**Implementation:** `SaladConverter.calculateTotalNutrition()` (lines 175-217)

### Feature 2: Flavor Profiling System

**Concept:**
Each food has 4 flavor attributes rated 0-10:
- **Crunch:** Texture/crunchiness
- **Punch:** Flavor intensity/spiciness
- **Sweet:** Sweetness level
- **Savory:** Umami/savory flavor

**Use Cases:**
- Help users balance salad flavors
- Suggest complementary ingredients
- Visualize flavor profiles (radar charts)

**Calculation:**
Same scaling approach as nutrition:
```
totalCrunch = sum((crunch × grams) / 100) for all ingredients
```

**Frontend Display:**
```tsx
<div>
  <span>Crunch: {salad.totalCrunch}/10</span>
  <span>Punch: {salad.totalPunch}/10</span>
  <span>Sweet: {salad.totalSweet}/10</span>
  <span>Savory: {salad.totalSavory}/10</span>
</div>
```

### Feature 3: Password Reset via Email

**Security Considerations:**

1. **Token Generation:** UUID v4 (cryptographically random)
2. **Token Storage:** Hashed? No, stored plain (single-use, short-lived)
3. **Expiration:** 1 hour from generation
4. **Single-Use:** Marked as `used=true` after consumption
5. **User Enumeration Prevention:** Always return success message
6. **Token Cleanup:** All tokens deleted after successful reset
7. **Rate Limiting:** Not implemented (future enhancement)

**Email Template:**
```
Subject: Password Reset Request

Click the link below to reset your password:
https://your-app.com/reset-password?token=<UUID>

This link will expire in 1 hour.

If you did not request a password reset, please ignore this email.
```

**Security Enhancement Ideas:**
- Rate limit password reset requests (e.g., max 3 per hour per email)
- Hash tokens in database (requires rainbow table protection)
- Send notification email when password is successfully changed
- Require old password confirmation for password changes (when logged in)

### Feature 4: Soft Delete Pattern

**Why Soft Delete?**
- Data recovery capability
- Audit trail preservation
- Foreign key integrity maintenance
- Gradual data retirement

**Implementation:**

```java
// Soft delete
public void delete(String extid) {
    Food food = findByExtid(extid);
    food.setActive(ActiveEnum.INACTIVE);
    food.setDeletedAt(LocalDateTime.now());
    foodDbService.update(food);
}

// Query only active records
List<FoodDb> findByActiveOrderByNameAsc(ActiveEnum active);
```

**Database:**
```sql
-- All tables have:
active INT DEFAULT 1       -- 1=active, 0=inactive
deleted_at DATETIME        -- NULL=active, timestamp=deleted
```

**Hard Delete (when needed):**
```java
foodRepository.deleteById(id);  // Physical deletion
```

---

## Security Considerations

### 1. Authentication & Authorization

**Current Implementation:**
- JWT-based stateless authentication
- BCrypt password hashing (10 rounds, auto-salted)
- Role-based authorization (USER, ADMIN)
- 24-hour token expiration

**Limitations:**
- No token revocation mechanism
- No refresh tokens
- No MFA support

**Recommendations:**
- Implement token blacklist for logout
- Add refresh token flow for extended sessions
- Add MFA (TOTP or SMS) for sensitive operations
- Implement account lockout after failed login attempts

### 2. Input Validation

**Current Protections:**
- JSR-303 Bean Validation (`@NotBlank`, `@Email`, etc.)
- Request DTO validation in controllers
- SQL injection protection via JPA parameterized queries
- XSS protection (React auto-escapes output)

**Example:**
```java
public class RequestLogin {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
```

### 3. Data Protection

**In Transit:**
- HTTPS enforced (via AWS ALB or CloudFront)
- TLS 1.2+ required
- STARTTLS for email (SMTP)

**At Rest:**
- Passwords: BCrypt hashed (never stored plain text)
- Database: RDS encryption at rest (optional)
- Backups: Encrypted snapshots

**In Code:**
- No credentials in source code (use env vars)
- `.env` file git-ignored
- Secrets managed via AWS Secrets Manager (recommended)

### 4. API Security

**Rate Limiting:**
- Not currently implemented
- Recommendation: Use AWS WAF or Spring Security rate limiting

**CORS:**
- Configured to allow specific origins only
- Credentials allowed (for cookies, if added)
- Preflight caching (1 hour)

**CSRF:**
- Disabled (not needed for stateless JWT)
- If adding cookies, re-enable CSRF protection

### 5. Database Security

**Access Control:**
- RDS security group restricts access to EB only
- Principle of least privilege (app user has only needed permissions)
- No public access to RDS

**Injection Prevention:**
- JPA/Hibernate parameterized queries
- No raw SQL execution (except in Liquibase migrations)
- Input sanitization via Bean Validation

### 6. Common Vulnerabilities

**XSS (Cross-Site Scripting):**
- Protection: React auto-escapes JSX output
- Caution: Avoid `dangerouslySetInnerHTML`

**SQL Injection:**
- Protection: JPA parameterized queries
- No dynamic SQL construction

**CSRF (Cross-Site Request Forgery):**
- Not applicable (stateless JWT, no cookies)
- If adding cookie-based auth, enable CSRF protection

**Authentication Bypass:**
- Protection: All `/api/**` routes require JWT (except `/api/auth/**`)
- `ProtectedRoute` component on frontend

**Session Fixation:**
- Not applicable (stateless sessions)

**Sensitive Data Exposure:**
- Passwords never returned in API responses
- Internal IDs hidden (use `extid` instead)
- Error messages sanitized (no stack traces to client)

### 7. Security Checklist

**Development:**
- [ ] Never commit `.env` file
- [ ] Use environment variables for secrets
- [ ] Validate all user input
- [ ] Sanitize error messages
- [ ] Log security events

**Production:**
- [ ] Enable HTTPS (ALB/CloudFront)
- [ ] Rotate JWT secret regularly
- [ ] Use AWS Secrets Manager
- [ ] Enable RDS encryption
- [ ] Configure AWS WAF
- [ ] Set up CloudWatch alarms
- [ ] Enable access logging
- [ ] Regular security audits

---

## Performance & Optimization

### Backend Optimizations

**1. Database Query Optimization**

**N+1 Query Problem:**
```java
// Bad: N+1 queries
List<Salad> salads = saladRepository.findAll();
salads.forEach(s -> s.getFoodIngredients().size());  // Triggers lazy load

// Good: Eager fetch with JOIN
@Query("SELECT s FROM SaladDb s LEFT JOIN FETCH s.foodIngredients WHERE s.active = :active")
List<SaladDb> findAllWithIngredients(@Param("active") ActiveEnum active);
```

**Indexing:**
- Primary keys (auto-indexed)
- Foreign keys (indexed)
- Unique constraints (`extid`, `code`, `username`, etc.)
- Custom indexes on frequently queried columns

**Connection Pooling (HikariCP):**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
```

**2. Caching Strategy**

**Current:** No caching implemented

**Recommendations:**
```java
// Spring Cache with Redis
@Cacheable(value = "foods", key = "#extid")
public Food findByExtid(String extid) {
    return foodDbService.findByExtid(extid);
}

@CacheEvict(value = "foods", key = "#extid")
public void delete(String extid) {
    // ...
}
```

**3. Pagination**

**Current:** All endpoints return full lists

**Recommendation:**
```java
@GetMapping
public Page<ResponseFood> getAll(Pageable pageable) {
    Page<Food> foods = foodService.findAll(pageable);
    return foods.map(foodConverter::toResponse);
}

// Client: GET /api/food?page=0&size=20&sort=name,asc
```

### Frontend Optimizations

**1. Code Splitting**

**Current:** Single bundle

**Recommendation:**
```tsx
// Lazy load routes
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Foods = lazy(() => import('./pages/Foods'));

<Suspense fallback={<LoadingSpinner />}>
  <Route path="/" element={<Dashboard />} />
</Suspense>
```

**2. TanStack Query Optimizations**

```typescript
// Stale-while-revalidate pattern
const { data } = useQuery({
  queryKey: ['foods'],
  queryFn: foodApi.getAll,
  staleTime: 5 * 60 * 1000,  // 5 minutes
  cacheTime: 10 * 60 * 1000,  // 10 minutes
  refetchOnWindowFocus: false
});

// Prefetch on hover
const queryClient = useQueryClient();
const prefetchFood = (extid: string) => {
  queryClient.prefetchQuery({
    queryKey: ['food', extid],
    queryFn: () => foodApi.getById(extid)
  });
};
```

**3. Image Optimization**

**Current:** Static images in `public/`

**Recommendations:**
- Use WebP format
- Lazy load images
- Responsive images (`srcset`)
- CDN delivery (CloudFront)

**4. Bundle Size Optimization**

```bash
# Analyze bundle
npm run build
npx vite-bundle-visualizer

# Tree-shaking: Import only what's needed
import { useState } from 'react';  // Good
import * as React from 'react';   // Bad (imports everything)
```

### Database Performance

**1. Indexing Strategy**

```sql
-- Indexes created via Liquibase
CREATE INDEX idx_food_category ON food(category);
CREATE INDEX idx_food_active ON food(active);
CREATE INDEX idx_salad_user_extid ON salad(user_extid);
CREATE INDEX idx_password_reset_token_token ON password_reset_token(token);
```

**2. Query Optimization**

```sql
-- Efficient query with proper indexes
EXPLAIN SELECT * FROM food WHERE active = 1 AND category = 'Greens';

-- Uses: idx_food_active, idx_food_category
```

**3. Database Monitoring**

**AWS RDS Performance Insights:**
- Slow query log
- Connection metrics
- CPU/memory usage
- Disk I/O

### Monitoring & Metrics

**Application Metrics (Spring Actuator):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  metrics:
    export:
      cloudwatch:
        enabled: true
```

**CloudWatch Metrics:**
- Request count
- Response time (p50, p95, p99)
- Error rate (4xx, 5xx)
- Database connection pool usage
- JVM memory/GC metrics

**Logging:**
```yaml
logging:
  level:
    com.seibel.cpss: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  file:
    name: logs/cpss.log
    max-size: 10MB
    max-history: 30
```

---

## Troubleshooting Guide

### Common Issues

#### 1. Database Connection Failures

**Symptoms:**
```
Cannot create PoolableConnectionFactory
Communications link failure
```

**Causes & Solutions:**

**Cause:** Incorrect credentials
```bash
# Check .env file
cat .env

# Verify MySQL credentials
mysql -h localhost -u cpss_user -p cpss
```

**Cause:** MySQL not running
```bash
# Linux/Mac
sudo systemctl status mysql

# Start MySQL
sudo systemctl start mysql

# Docker
docker ps
docker start cpss-mysql
```

**Cause:** RDS security group (AWS)
- Go to RDS → Security Groups
- Add inbound rule: Port 3306 from EB security group

#### 2. Liquibase Migration Failures

**Symptoms:**
```
Liquibase Update Failed: Validation Failed
```

**Solutions:**

**Force unlock (if stuck):**
```sql
DELETE FROM DATABASECHANGELOGLOCK;
```

**Rollback failed migration:**
```bash
./gradlew rollbackCount -PliquibaseCommandValue=1
```

**Clear and restart:**
```sql
DROP DATABASE cpss;
CREATE DATABASE cpss;
```
```bash
./gradlew update
```

#### 3. JWT Token Issues

**Symptoms:**
- 401 Unauthorized on every request
- "JWT signature does not match"

**Causes & Solutions:**

**Cause:** Token expired (24hr)
- Solution: Login again to get new token

**Cause:** JWT secret changed
- Solution: Logout and login (old tokens invalid)

**Cause:** Token not sent
```typescript
// Check localStorage
console.log(localStorage.getItem('token'));

// Verify Authorization header
console.log(config.headers.Authorization);  // Should be "Bearer <token>"
```

#### 4. CORS Errors

**Symptoms:**
```
Access to XMLHttpRequest blocked by CORS policy
```

**Solutions:**

**Development:**
- Verify Vite proxy config (`vite.config.ts`)
- Use `/api` relative path (not `http://localhost:8080/api`)

**Production:**
- Check `cors.allowed.origins` environment variable
- Must include frontend domain

```yaml
# Backend application.yml
cors:
  allowed:
    origins: https://your-frontend-domain.com
```

#### 5. Email Not Sending

**Symptoms:**
- Password reset email not received
- "Authentication failed" in logs

**Causes & Solutions:**

**Cause:** Gmail app password not configured
- Enable 2FA on Gmail
- Generate app-specific password (not regular password)
- Update `MAIL_PASSWORD` environment variable

**Cause:** SMTP blocked
- Check firewall allows port 587
- Verify `MAIL_HOST` and `MAIL_PORT` correct

**Test email:**
```java
@Autowired
private JavaMailSender mailSender;

SimpleMailMessage message = new SimpleMailMessage();
message.setFrom("test@example.com");
message.setTo("recipient@example.com");
message.setSubject("Test");
message.setText("Hello");
mailSender.send(message);
```

#### 6. Frontend Build Failures

**Symptoms:**
```
npm ERR! code ELIFECYCLE
```

**Solutions:**

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Clear npm cache
npm cache clean --force

# Update npm
npm install -g npm@latest

# Check Node version
node -v  # Should be 18+
```

#### 7. EB Deployment Failures

**Symptoms:**
- Deployment times out
- Health check failing

**Solutions:**

**Check logs:**
```bash
eb logs
eb health --refresh
```

**Common issues:**
- Port mismatch (app must run on port 8080 or $PORT)
- Memory limit exceeded (increase instance type)
- Database connection timeout (check RDS security group)
- Missing environment variables

**Verify JAR:**
```bash
# Test JAR locally
java -jar build/libs/cpss-0.0.2-SNAPSHOT.jar

# Check JAR contents
jar tf build/libs/cpss-0.0.2-SNAPSHOT.jar | grep static
```

### Debug Mode

**Backend:**
```yaml
# application.yml
logging:
  level:
    com.seibel.cpss: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

**Frontend:**
```typescript
// api.ts
apiClient.interceptors.request.use(config => {
  console.log('[API Request]', config.method, config.url, config.data);
  return config;
});

apiClient.interceptors.response.use(
  response => {
    console.log('[API Response]', response.status, response.data);
    return response;
  },
  error => {
    console.error('[API Error]', error.response);
    return Promise.reject(error);
  }
);
```

---

## Appendices

### Appendix A: Glossary

**Active Enum:** Integer flag (1=active, 0=soft deleted)

**BCrypt:** Password hashing algorithm with automatic salting

**Domain Model:** Business logic layer representation (POJO)

**DTO (Data Transfer Object):** Object for transferring data between layers/tiers

**Extid:** External UUID identifier exposed in APIs (hides internal DB IDs)

**JPA Entity:** Database layer representation with Hibernate annotations

**JWT (JSON Web Token):** Stateless authentication token

**Liquibase:** Database migration/versioning tool

**Soft Delete:** Marking records as inactive instead of physical deletion

**Stateless Authentication:** No server-side session storage (JWT-based)

### Appendix B: File Locations Reference

| Component | Location |
|-----------|----------|
| Main Application | `/src/main/java/com/seibel/cpss/CpssApplication.java` |
| Security Config | `/src/main/java/com/seibel/cpss/config/SecurityConfig.java` |
| Controllers | `/src/main/java/com/seibel/cpss/web/controller/` |
| Services | `/src/main/java/com/seibel/cpss/service/` |
| Repositories | `/src/main/java/com/seibel/cpss/database/db/repository/` |
| Domain Models | `/src/main/java/com/seibel/cpss/common/domain/` |
| JPA Entities | `/src/main/java/com/seibel/cpss/database/db/entity/` |
| DB Migrations | `/src/main/resources/db/changelog/changes/` |
| Application Config | `/src/main/resources/application.yml` |
| Frontend App | `/frontend/src/App.tsx` |
| API Client | `/frontend/src/services/api.ts` |
| Pages | `/frontend/src/pages/` |
| Components | `/frontend/src/components/` |
| Build Config | `/build.gradle` |
| Frontend Config | `/frontend/vite.config.ts` |
| Environment Vars | `/.env` (git-ignored) |

### Appendix C: Useful Commands

**Backend:**
```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun

# Test
./gradlew test

# Database migrations
./gradlew update
./gradlew rollbackCount -PliquibaseCommandValue=1

# Deploy build
./gradlew buildDeployment

# Kill frontend dev server
./gradlew killFrontend
```

**Frontend:**
```bash
# Install dependencies
npm install

# Dev server
npm run dev

# Build
npm run build

# Lint
npm run lint

# Preview production build
npm run preview
```

**Docker (MySQL):**
```bash
# Start
docker start cpss-mysql

# Stop
docker stop cpss-mysql

# Logs
docker logs cpss-mysql

# Shell
docker exec -it cpss-mysql mysql -u root -p
```

**AWS:**
```bash
# Elastic Beanstalk
eb init
eb create
eb deploy
eb status
eb health
eb logs
eb open
eb terminate

# Environment variables
eb setenv KEY=value

# RDS
aws rds describe-db-instances
```

### Appendix D: External Resources

**Documentation:**
- Spring Boot: https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security
- React: https://react.dev
- Vite: https://vitejs.dev
- TanStack Query: https://tanstack.com/query
- Liquibase: https://docs.liquibase.com

**Tools:**
- Postman Collection: `/documents/postman/cpss.postman_collection.json`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- AWS Console: https://console.aws.amazon.com

**Support:**
- GitHub Issues: (repository URL)
- Email: (support email)

---

**End of Technical Documentation**

For questions or clarifications, please refer to the source code or contact the development team.

**Document Version:** 1.0
**Last Updated:** February 13, 2026
**Maintained By:** CPSS Development Team
