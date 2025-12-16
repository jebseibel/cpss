# CPSS - High Level Architecture

## Project Overview
Food/salad management system with JWT authentication built on Spring Boot + React.

## Tech Stack
- **Backend**: Java 21, Spring Boot 3.x, Spring Security, JWT
- **Frontend**: Vite + React + Tailwind
- **Database**: MySQL (AWS RDS) with Liquibase migrations
- **Deployment**: AWS Elastic Beanstalk

---

## 1. DATABASE LAYER

### Entities
- **Company** - Business entities
- **User** - Authentication (username, password, email, role)
- **Food** - Core entity with category, subcategory, foundation flag
- **Flavor** - Taste profiles (crunch, punch, sweet, savory ratings)
- **Nutrition** - Nutritional data (carbs, fat, protein, sugar)
- **Serving** - Serving sizes (cup, quarter, tablespoon, teaspoon, gram)

### Relationships
- Food has one-to-one with Flavor, Nutrition, Serving
- All entities inherit from BaseDb (id, extid, timestamps, soft delete)

### Components
- **Repositories**: Spring Data JPA interfaces
- **DB Services**: CRUD operations with soft delete support
- **Mappers**: Entity ↔ Domain model conversion (ModelMapper)
- **Migrations**: Liquibase changesets with CSV data loading

---

## 2. COMMON LAYER

### Domain Models
Pure Java business objects mirroring database entities (Company, Food, Flavor, Nutrition, Serving)

### Security
- **JwtUtil**: Token generation/validation (24hr expiration)
- **JwtAuthenticationFilter**: Request interception and token validation
- **CustomUserDetailsService**: User loading for Spring Security

### Configuration
- **SecurityConfig**: JWT auth, BCrypt passwords, stateless sessions
- **WebConfig**: CORS for localhost:5173, localhost:3000

### Exceptions
- **ResourceNotFoundException** (404)
- **ValidationException** (400)
- **ResourceAlreadyExistsException** (409)
- **ServiceException** (500)
- **GlobalExceptionHandler**: Centralized error handling

### Utilities
- **CodeGenerator**: Auto-generate codes from names
- **ActiveEnum**: ACTIVE/INACTIVE status enumeration

---

## 3. REST API LAYER

### Controllers

**AuthController** (`/api/auth`)
- `POST /login` - Username/password authentication → JWT token
- `POST /register` - User registration → JWT token

**CompanyController** (`/api/company`)
- Standard CRUD with pagination, sorting, filtering by active status

**FoodController** (`/api/food`)
- CRUD for foods with nested flavor, nutrition, serving data

**SaladController** (`/api/salad`)
- `POST /build` - Build custom salad from ingredients → aggregated nutrition/flavor

**FlavorController, NutritionController, ServingController**
- CRUD operations for respective entities

### DTOs
- **Request DTOs**: Jakarta Validation annotations (RequestLogin, RequestCompanyCreate, etc.)
- **Response DTOs**: Clean API responses (ResponseAuth, ResponseCompany, ErrorResponse, etc.)
- **Converters**: Request/Response ↔ Domain model transformation

---

## Data Flow

```
HTTP Request
    ↓
Controller (validate request DTO → domain model)
    ↓
Business Service (validation, business logic)
    ↓
DB Service (CRUD operations)
    ↓
Repository (JPA/Hibernate)
    ↓
MySQL Database
```

## Authentication Flow

```
1. POST /api/auth/login → JWT token
2. Client stores token (localStorage)
3. Client sends: Authorization: Bearer <token>
4. JwtAuthenticationFilter validates token
5. Request proceeds to protected endpoints
```

## Key Features
- Soft deletes (deletedAt timestamp, active status)
- Pagination with configurable limits
- Auto-code generation for entities
- Aggregated salad building with nutritional calculations
- Comprehensive validation and error handling
- Swagger/OpenAPI documentation
