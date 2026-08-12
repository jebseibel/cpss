# CPSS — Custom Prepared Salad System

A full-stack web app for building custom salads and food mixtures with real-time nutritional calculation.

- **Backend:** Java 21 + Spring Boot 3.5 (REST API)
- **Frontend:** React 19 + TypeScript + Vite + Tailwind CSS
- **Database:** MySQL (AWS RDS in production), Liquibase migrations
- **Auth:** JWT-based, stateless

## Quick Start

```bash
# Backend
./gradlew bootRun

# Frontend
cd frontend && npm run dev
```

Copy `.env.example` (or see technical docs) to create your local `.env` — never commit real credentials.

See the technical documentation for architecture, API reference, and deployment details.
