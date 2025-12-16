# Spring Boot + React Authentication Project

## Project Overview
- Backend: Java Spring Boot + Gradle (REST API on AWS Elastic Beanstalk)
- Frontend: Vite + React + Tailwind
- Database: AWS RDS MySQL

## Tech Stack
- Java 21+
- Spring Boot 3.x
- Spring Security
- JWT tokens
- React 18
- Vite
- Tailwind CSS

## Architecture Notes
- RESTful API backend
- JWT tokens for stateless auth
- Token stored in localStorage (frontend)
- Basic security (no social login, MFA, etc.)

FRONT END NOTES:
- For any Front End work, we can not use Modals.

Notes:
FE - meant for the frontend
BE - meant for the backend
Rebuild the database - means to trigger the n8n workflow

delete or rebuild the database means; run the docker 'n8n' webhook: http://localhost:5678/webhook/clear-cpss-db
It is a GET
