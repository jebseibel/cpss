# CPSS Technical Documentation - Index

This comprehensive technical documentation is split across three files for easier navigation and maintenance.

## Documentation Structure

### Part 1: Foundation & Architecture
**File:** `TECHNICAL_DOCUMENTATION.md`

**Contents:**
- Executive Summary
- System Architecture (high-level diagrams)
- Technology Stack (complete list of all technologies)
- Project Structure (directory layouts)
- Backend Architecture (layered architecture, design patterns)
- Frontend Architecture (component hierarchy, state management)
- Database Schema (ERD, table schemas, relationships)

**Best For:** New developers, architects, technical leadership

---

### Part 2: Security, APIs & Deployment
**File:** `TECHNICAL_DOCUMENTATION_PART2.md`

**Contents:**
- Authentication & Security (JWT flows, password reset)
- API Reference (complete endpoint documentation)
- Data Flow (sequence diagrams, calculation formulas)
- Deployment Architecture (AWS setup, infrastructure)

**Best For:** Backend developers, DevOps engineers, API consumers

---

### Part 3: Development & Operations
**File:** `TECHNICAL_DOCUMENTATION_PART3.md`

**Contents:**
- Development Setup (prerequisites, installation)
- Build & Deployment Process (local builds, CI/CD)
- Configuration Management (environment variables, profiles)
- Key Features Deep Dive (implementation details)
- Security Considerations (vulnerabilities, best practices)
- Performance & Optimization (caching, indexing, monitoring)
- Troubleshooting Guide (common issues, debug mode)
- Appendices (glossary, commands, resources)

**Best For:** Developers setting up locally, operations teams, troubleshooting

---

## Quick Start Guide

### For New Developers

1. **Read First:** Executive Summary in Part 1
2. **Setup Environment:** Development Setup in Part 3
3. **Understand Architecture:** Backend/Frontend Architecture in Part 1
4. **Run Locally:** Follow Development Setup steps in Part 3
5. **Explore APIs:** API Reference in Part 2

### For DevOps/Infrastructure

1. **Read First:** Deployment Architecture in Part 2
2. **AWS Setup:** Deployment section in Part 3
3. **Configuration:** Configuration Management in Part 3
4. **Monitoring:** Performance & Optimization in Part 3
5. **Troubleshooting:** Troubleshooting Guide in Part 3

### For Frontend Developers

1. **Read First:** Frontend Architecture in Part 1
2. **Setup:** Development Setup (Frontend section) in Part 3
3. **API Integration:** API Reference in Part 2
4. **State Management:** Frontend Architecture in Part 1
5. **Build Process:** Build & Deployment in Part 3

### For Backend Developers

1. **Read First:** Backend Architecture in Part 1
2. **Database Schema:** Database Schema in Part 1
3. **API Design:** API Reference in Part 2
4. **Security:** Authentication & Security in Part 2
5. **Setup:** Development Setup (Backend section) in Part 3

---

## Documentation Highlights

### Key Architectural Decisions

**Why JWT instead of sessions?**
- Stateless architecture enables horizontal scaling
- No server-side session storage needed
- Simplifies microservices architecture (future-proof)
- Trade-off: Cannot revoke tokens before expiration

**Why Liquibase for migrations?**
- Version-controlled database schema
- Rollback capabilities for failed migrations
- Multi-environment support (dev, staging, prod)
- Audit trail of all schema changes

**Why monolithic deployment (frontend + backend)?**
- Simplified deployment (single JAR file)
- Reduced infrastructure complexity
- Suitable for small-to-medium applications
- Easy to split later if needed

### Critical File Locations

| What You Need | Where to Find It |
|---------------|------------------|
| Main application entry point | `/src/main/java/com/seibel/cpss/CpssApplication.java` |
| Security configuration | `/src/main/java/com/seibel/cpss/config/SecurityConfig.java` |
| JWT implementation | `/src/main/java/com/seibel/cpss/security/JwtUtil.java` |
| Password reset logic | `/src/main/java/com/seibel/cpss/service/PasswordResetService.java` |
| Database migrations | `/src/main/resources/db/changelog/changes/` |
| Frontend routing | `/frontend/src/App.tsx` |
| API client | `/frontend/src/services/api.ts` |
| Build configuration | `/build.gradle` |
| Environment variables | `/.env` (git-ignored, create locally) |

### Most Common Tasks

**Run application locally:**
```bash
# Terminal 1 - Backend
./gradlew bootRun

# Terminal 2 - Frontend
cd frontend && npm run dev

# Access: http://localhost:5173
```

**Deploy to AWS:**
```bash
./gradlew buildDeployment
eb deploy
```

**Reset database:**
```bash
curl http://localhost:5678/webhook/clear-cpss-db  # n8n webhook
# OR
./gradlew update  # Re-run migrations
```

**Debug API issues:**
```bash
# Check backend logs
./gradlew bootRun --info

# Check network tab in browser
# Verify Authorization header contains: Bearer <token>
```

---

## Technical Stack Summary

**Backend:** Java 21 + Spring Boot 3.5.7 + MySQL 8.0
**Frontend:** React 19 + TypeScript + Vite + Tailwind CSS
**Authentication:** JWT (stateless, 24hr expiry)
**Database:** AWS RDS MySQL with Liquibase migrations
**Deployment:** AWS Elastic Beanstalk (single JAR with embedded frontend)
**Email:** SMTP via Gmail

---

## Feature Overview

### Core Features

1. **User Authentication**
   - JWT-based login/register
   - Password reset via email
   - Username reminder via email
   - BCrypt password hashing

2. **Food Database**
   - 100+ foods with nutritional data
   - Categorization (category, subcategory)
   - Flavor profiling (crunch, punch, sweet, savory)
   - Foundation/mixable flags

3. **Salad Builder**
   - Drag-and-drop ingredient selection
   - Real-time nutritional calculation
   - Gram-based portioning
   - Flavor balance visualization

4. **Mixture Creator**
   - Custom food mixtures
   - Ingredient tracking
   - Nutritional aggregation

5. **Nutritional Analysis**
   - Per-100g standardization
   - Automatic scaling by weight
   - Macro tracking (carbs, protein, fat)
   - Micronutrient tracking (vitamins D, E, fiber)
   - Calorie calculation from macros

---

## Security Overview

**Authentication:** JWT tokens (HS256, 24hr expiry)
**Password Storage:** BCrypt (10 rounds, auto-salted)
**Password Reset:** UUID tokens (1hr expiry, single-use)
**Input Validation:** JSR-303 Bean Validation
**SQL Injection Prevention:** JPA parameterized queries
**XSS Prevention:** React auto-escaping
**CORS:** Configured for specific origins

**Production Recommendations:**
- Use HTTPS (via AWS ALB)
- Rotate JWT secret regularly
- Use AWS Secrets Manager for credentials
- Enable AWS WAF for rate limiting
- Implement token refresh flow
- Add MFA for sensitive operations

---

## Performance Considerations

**Current:**
- No caching layer
- No pagination (all endpoints return full lists)
- Single database instance
- No CDN for static assets

**Optimization Opportunities:**
- Add Redis for caching frequently accessed data
- Implement pagination for large lists
- Add database read replicas
- Use CloudFront CDN for static assets
- Lazy load React routes
- Optimize bundle size (code splitting)

**Monitoring:**
- CloudWatch for application metrics
- RDS Performance Insights for database queries
- Spring Actuator for JVM metrics
- Access logs for request patterns

---

## Support & Contribution

**Troubleshooting:**
- See Part 3: Troubleshooting Guide for common issues
- Check application logs: `eb logs` or `./gradlew bootRun`
- Verify environment variables in `.env` file

**Contributing:**
- Follow existing code patterns (layered architecture)
- Write tests for new features
- Update Liquibase migrations for schema changes
- Document API changes in this technical documentation

**Questions:**
- Check Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Review Postman collection: `/documents/postman/cpss.postman_collection.json`
- Contact development team

---

## Version History

**v0.0.2-SNAPSHOT (Current)**
- Password reset functionality
- Email service integration
- Enhanced logging
- Stabilized salad builder
- Added mixture shop feature

**v0.0.1**
- Initial release
- Basic CRUD for foods, salads, mixtures
- JWT authentication
- Database schema foundations

---

## Next Steps

After reading this documentation, you should:

1. **Developers:** Set up local environment (Part 3)
2. **Architects:** Review system architecture (Part 1)
3. **DevOps:** Understand deployment process (Part 3)
4. **API Consumers:** Study API reference (Part 2)

For detailed information on any topic, refer to the specific documentation part listed above.

---

**Documentation Maintained By:** CPSS Development Team
**Last Updated:** February 13, 2026
**Version:** 1.0
