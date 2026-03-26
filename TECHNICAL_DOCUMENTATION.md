# CPSS Technical Documentation

**Version:** 0.0.2-SNAPSHOT
**Last Updated:** February 13, 2026
**Project Type:** Full-Stack Web Application (Nutrition/Salad Builder Platform)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [System Architecture](#system-architecture)
3. [Technology Stack](#technology-stack)
4. [Project Structure](#project-structure)
5. [Backend Architecture](#backend-architecture)
6. [Frontend Architecture](#frontend-architecture)
7. [Database Schema](#database-schema)
8. [Authentication & Security](#authentication--security)
9. [API Reference](#api-reference)
10. [Data Flow](#data-flow)
11. [Deployment Architecture](#deployment-architecture)
12. [Development Setup](#development-setup)
13. [Build & Deployment Process](#build--deployment-process)
14. [Configuration Management](#configuration-management)
15. [Key Features Deep Dive](#key-features-deep-dive)
16. [Security Considerations](#security-considerations)
17. [Performance & Optimization](#performance--optimization)
18. [Troubleshooting Guide](#troubleshooting-guide)
19. [Appendices](#appendices)

---

## Executive Summary

### Overview

CPSS (Custom Prepared Salad System) is a full-stack web application designed to help users create, customize, and track nutritional information for salads and food mixtures. The system provides a comprehensive food database with detailed nutritional information, allowing users to build custom salads, track macronutrients, and manage their dietary preferences.

### Key Capabilities

- **User Authentication**: Secure JWT-based authentication with password reset functionality
- **Food Database**: Comprehensive database of foods with nutritional information (calories, macros, vitamins)
- **Salad Builder**: Interactive interface to create custom salads with real-time nutritional calculations
- **Mixture Creator**: Build custom food mixtures with ingredient tracking
- **Nutritional Analysis**: Automatic calculation of total nutrition from ingredients (scaled by weight)
- **Flavor Profiling**: Track sensory attributes (crunch, punch, sweet, savory) for foods

### Target Users

- Health-conscious individuals tracking nutrition
- Meal planners and nutritionists
- Users interested in customizing salad recipes
- Anyone seeking detailed nutritional information

### System Boundaries

**In Scope:**
- RESTful API backend with MySQL database
- React-based SPA frontend
- JWT authentication
- CRUD operations for foods, salads, mixtures, and nutrition data
- Email notifications (username reminders, password resets)

**Out of Scope:**
- Social login (OAuth, SSO)
- Multi-factor authentication (MFA)
- Mobile native applications
- Real-time collaboration features
- Payment processing

---

## System Architecture

### High-Level Architecture

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│                 │         │                 │         │                 │
│  React Frontend │◄───────►│  Spring Boot    │◄───────►│   MySQL RDS     │
│  (Vite + TS)    │  HTTP   │   REST API      │  JDBC   │   Database      │
│                 │  JSON   │   (Java 21)     │         │                 │
└─────────────────┘         └─────────────────┘         └─────────────────┘
        │                            │
        │                            │
        │                            ▼
        │                   ┌─────────────────┐
        │                   │                 │
        │                   │  Email Service  │
        │                   │   (SMTP/Gmail)  │
        │                   │                 │
        │                   └─────────────────┘
        │
        ▼
┌─────────────────┐
│  localStorage   │
│  (JWT Token)    │
└─────────────────┘
```

### Component Diagram

```
┌────────────────────────────────────────────────────────────┐
│                     CPSS Application                       │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │               Frontend Layer (React)                 │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │  - Pages (Login, Dashboard, Salads, Foods, etc.)    │ │
│  │  - Components (Layout, ProtectedRoute)              │ │
│  │  - Services (API Client, Auth Helpers)              │ │
│  │  - State Management (TanStack Query)                │ │
│  └──────────────────────────────────────────────────────┘ │
│                           │                                │
│                           │ HTTP/JSON                      │
│                           ▼                                │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              Backend Layer (Spring Boot)             │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │  ┌────────────────────────────────────────────────┐  │ │
│  │  │  Web Layer (Controllers)                       │  │ │
│  │  │  - AuthController, FoodController, etc.        │  │ │
│  │  └────────────────────────────────────────────────┘  │ │
│  │  ┌────────────────────────────────────────────────┐  │ │
│  │  │  Security Layer                                │  │ │
│  │  │  - JWT Filter, UserDetailsService              │  │ │
│  │  └────────────────────────────────────────────────┘  │ │
│  │  ┌────────────────────────────────────────────────┐  │ │
│  │  │  Service Layer                                 │  │ │
│  │  │  - Business Logic (FoodService, etc.)          │  │ │
│  │  └────────────────────────────────────────────────┘  │ │
│  │  ┌────────────────────────────────────────────────┐  │ │
│  │  │  Database Layer                                │  │ │
│  │  │  - Repositories, Entities, Mappers             │  │ │
│  │  └────────────────────────────────────────────────┘  │ │
│  └──────────────────────────────────────────────────────┘ │
│                           │                                │
│                           │ JPA/Hibernate                  │
│                           ▼                                │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              Data Layer (MySQL RDS)                  │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │  Tables: users, food, nutrition, salad,              │ │
│  │          mixture, password_reset_token, etc.         │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

### Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      AWS Cloud                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌────────────────────────────────┐                        │
│  │  AWS Elastic Beanstalk         │                        │
│  ├────────────────────────────────┤                        │
│  │  - Java 21 Platform            │                        │
│  │  - Spring Boot JAR             │                        │
│  │  - Embedded Frontend (static)  │────┐                   │
│  │  - Port: 8080                  │    │                   │
│  └────────────────────────────────┘    │                   │
│               │                         │                   │
│               │                         │                   │
│               ▼                         ▼                   │
│  ┌────────────────────────────┐  ┌──────────────────┐      │
│  │  AWS RDS MySQL             │  │  SMTP Service    │      │
│  ├────────────────────────────┤  │  (Gmail)         │      │
│  │  - Database: cpss          │  │  - Port: 587     │      │
│  │  - Port: 3306              │  │  - TLS/STARTTLS  │      │
│  │  - Liquibase Migrations    │  └──────────────────┘      │
│  └────────────────────────────┘                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Key Architectural Decisions

**1. Monolithic Frontend Deployment**
- **Decision**: Frontend is built and bundled into the Spring Boot JAR as static resources
- **Rationale**: Simplifies deployment (single artifact), reduces infrastructure complexity, suitable for small-to-medium applications
- **Trade-off**: Less flexible than separate frontend deployment, but easier to maintain

**2. Stateless JWT Authentication**
- **Decision**: Use JWT tokens stored in localStorage instead of server-side sessions
- **Rationale**: Enables horizontal scaling, reduces server memory usage, simplifies architecture
- **Trade-off**: Cannot easily revoke tokens before expiration (24-hour expiry mitigates this)

**3. Liquibase for Database Migrations**
- **Decision**: Use Liquibase for version-controlled database schema management
- **Rationale**: Provides rollback capabilities, tracks migration history, supports multiple environments
- **Trade-off**: Additional dependency, but industry-standard solution

**4. Layered Architecture Pattern**
- **Decision**: Strict separation of concerns (Web → Service → Database layers)
- **Rationale**: Maintainability, testability, clear boundaries between layers
- **Trade-off**: More boilerplate code, but better long-term maintainability

---

## Technology Stack

### Backend Technologies

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Core programming language |
| Spring Boot | 3.5.7 | Application framework |
| Spring Security | (via Boot) | Authentication & authorization |
| Spring Data JPA | (via Boot) | Data access layer |
| Hibernate | (via JPA) | ORM implementation |
| JWT (jjwt) | 0.12.6 | Token-based authentication |
| Liquibase | 4.29.2 | Database migration tool |
| MySQL Connector | Latest | JDBC driver for MySQL |
| Lombok | 1.18.34 | Reduce boilerplate code |
| ModelMapper | 3.2.0 | Object mapping utility |
| SpringDoc OpenAPI | 2.6.0 | API documentation (Swagger) |
| Spring Mail | (via Boot) | Email functionality |
| Thymeleaf | (via Boot) | Email templates (if needed) |
| Gradle | (wrapper) | Build tool |

### Frontend Technologies

| Technology | Version | Purpose |
|-----------|---------|---------|
| React | 19.1.1 | UI framework |
| TypeScript | 5.9.3 | Type-safe JavaScript |
| Vite | 7.1.7 | Build tool & dev server |
| React Router | 7.9.5 | Client-side routing |
| TanStack Query | 5.90.6 | Server state management |
| Axios | 1.13.1 | HTTP client |
| React Hook Form | 7.66.0 | Form management |
| Zod | 4.1.12 | Schema validation |
| Tailwind CSS | 4.1.16 | Utility-first CSS framework |
| Recharts | 3.3.0 | Charting library |
| Lucide React | 0.552.0 | Icon library |

### Infrastructure & Services

| Service | Purpose |
|---------|---------|
| AWS Elastic Beanstalk | Application hosting |
| AWS RDS MySQL | Database hosting |
| Gmail SMTP | Email delivery |
| n8n Webhook | Database reset automation (dev) |

### Development Tools

| Tool | Purpose |
|------|---------|
| IntelliJ IDEA / VS Code | IDE |
| Git | Version control |
| Postman | API testing (collection included) |
| Spring Boot DevTools | Hot reload during development |

---

## Project Structure

### Root Directory Layout

```
cpss/
├── build/                      # Gradle build output
├── documents/                  # Project documentation
│   └── postman/               # Postman API collection
├── frontend/                   # React frontend application
│   ├── dist/                  # Frontend build output
│   ├── public/                # Static assets
│   ├── src/                   # Frontend source code
│   └── package.json           # NPM dependencies
├── gradle/                     # Gradle wrapper files
├── src/                       # Backend source code
│   ├── main/
│   │   ├── java/             # Java source files
│   │   └── resources/        # Configuration & migrations
│   └── test/                 # Test files
├── .env                       # Environment variables (local)
├── .gitignore                # Git ignore rules
├── build.gradle              # Gradle build configuration
├── gradlew                   # Gradle wrapper script (Unix)
├── gradlew.bat              # Gradle wrapper script (Windows)
├── README.md                # Project readme
└── settings.gradle          # Gradle settings
```

### Backend Source Structure

```
src/main/java/com/seibel/cpss/
├── CpssApplication.java           # Spring Boot entry point
├── common/                        # Shared domain models & utilities
│   ├── domain/                   # Domain entities (POJO)
│   │   ├── BaseDomain.java
│   │   ├── Food.java
│   │   ├── Nutrition.java
│   │   ├── Salad.java
│   │   ├── Mixture.java
│   │   ├── Company.java
│   │   ├── PasswordResetToken.java
│   │   └── SaladFoodIngredient.java
│   ├── enums/                    # Enumerations
│   │   ├── ActiveEnum.java
│   │   └── CompResult.java
│   ├── exceptions/               # Custom exceptions
│   │   ├── BaseServiceException.java
│   │   ├── ResourceNotFoundException.java
│   │   └── ValidationException.java
│   └── util/                     # Utility classes
│       └── CodeGenerator.java
├── config/                        # Spring configuration
│   ├── SecurityConfig.java       # Security & authentication config
│   └── WebConfig.java            # CORS & web MVC config
├── database/                      # Database layer
│   └── db/
│       ├── converter/            # JPA converters
│       ├── entity/               # JPA entities (DB models)
│       │   ├── BaseDb.java
│       │   ├── FoodDb.java
│       │   ├── UserDb.java
│       │   └── ...
│       ├── mapper/               # Domain ↔ Entity mappers
│       │   ├── FoodMapper.java
│       │   └── ...
│       ├── repository/           # Spring Data repositories
│       │   ├── FoodRepository.java
│       │   ├── UserRepository.java
│       │   └── ...
│       └── service/              # Database service layer
│           ├── FoodDbService.java
│           └── ...
├── loader/                        # Data loading utilities
│   ├── CsvParser.java
│   └── DataLoader.java
├── security/                      # Security components
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtUtil.java
├── service/                       # Business logic layer
│   ├── BaseService.java
│   ├── FoodService.java
│   ├── SaladService.java
│   ├── EmailService.java
│   └── PasswordResetService.java
└── web/                          # Web layer
    ├── controller/               # REST controllers
    │   ├── AuthController.java
    │   ├── FoodController.java
    │   ├── SaladController.java
    │   └── ...
    ├── request/                  # Request DTOs
    │   ├── RequestLogin.java
    │   ├── RequestFoodCreate.java
    │   └── ...
    ├── response/                 # Response DTOs
    │   ├── ResponseAuth.java
    │   ├── ResponseFood.java
    │   └── ...
    └── GlobalExceptionHandler.java
```

### Frontend Source Structure

```
frontend/src/
├── App.tsx                    # Main app component & routing
├── main.tsx                   # React entry point
├── index.css                  # Global styles
├── components/                # Reusable components
│   ├── Layout.tsx            # Main layout with navigation
│   └── ProtectedRoute.tsx    # Auth guard component
├── pages/                     # Page components (routes)
│   ├── Login.tsx
│   ├── ForgotPassword.tsx
│   ├── ForgotUsername.tsx
│   ├── ResetPassword.tsx
│   ├── Dashboard.tsx
│   ├── BeginHere.tsx
│   ├── MyStory.tsx
│   ├── Foods.tsx
│   ├── Nutrition.tsx
│   ├── Salads.tsx
│   ├── SaladBuilder.tsx
│   ├── Mixtures.tsx
│   ├── MakeMixture.tsx
│   └── MixtureShop.tsx
├── services/                  # API integration
│   └── api.ts                # Axios client & API functions
├── types/                     # TypeScript type definitions
│   └── api.ts                # API response/request types
└── lib/                       # Utility functions
    └── utils.ts              # Helper functions
```

### Database Migration Structure

```
src/main/resources/db/changelog/
├── db.changelog-master.yaml   # Master changelog file
└── changes/                   # Individual migration files
    ├── 001-company.yaml
    ├── 002-serving.yaml
    ├── 004-nutrition.yaml
    ├── 005-food.yaml
    ├── 006-users.yaml
    ├── 007-salad.yaml
    ├── 008-mixture.yaml
    ├── 009-mixture-ingredients.yaml
    ├── 010-rename-mixture-ingredient-quantity.yaml
    ├── 011-add-typical-serving-grams.yaml
    ├── 012-remove-serving-from-food.yaml
    ├── 013-add-vitamins-to-nutrition.yaml
    ├── 014-drop-serving-table.yaml
    ├── 015-populate-vitamin-defaults.yaml
    ├── 016-make-mixture-user-nullable.yaml
    ├── 017-add-fiber-to-nutrition.yaml
    ├── 018-create-salad-food-ingredient.yaml
    ├── 019-make-salad-user-nullable.yaml
    ├── 020-create-password-reset-token.yaml
    └── 100-load-csv-data.yaml
```

---

## Backend Architecture

### Layered Architecture Pattern

The backend follows a strict **4-layer architecture** to maintain separation of concerns and improve testability:

```
┌─────────────────────────────────────────┐
│         Web Layer (Controllers)          │  ← HTTP Requests/Responses
│  - REST endpoints                        │
│  - Request/Response DTOs                 │
│  - Input validation                      │
└─────────────────────────────────────────┘
                  ▼
┌─────────────────────────────────────────┐
│       Service Layer (Business Logic)     │  ← Business Rules
│  - Domain logic                          │
│  - Transactions                          │
│  - Domain models                         │
└─────────────────────────────────────────┘
                  ▼
┌─────────────────────────────────────────┐
│    Database Service Layer (DB Access)    │  ← Data Operations
│  - CRUD operations                       │
│  - Entity ↔ Domain mapping               │
│  - Repository calls                      │
└─────────────────────────────────────────┘
                  ▼
┌─────────────────────────────────────────┐
│     Data Layer (JPA Repositories)        │  ← Database Interaction
│  - Spring Data repositories              │
│  - JPA entities                          │
│  - Database queries                      │
└─────────────────────────────────────────┘
```

### Layer Responsibilities

**1. Web Layer** (`web/controller/`)
- Handles HTTP requests and responses
- Validates incoming request DTOs using JSR-303 annotations
- Converts domain models to response DTOs
- Delegates business logic to service layer
- **Does NOT** contain business logic or direct database access

**2. Service Layer** (`service/`)
- Implements business logic and rules
- Operates on domain models (not DB entities)
- Manages transactions (via `@Transactional`)
- Coordinates between multiple database services
- **Does NOT** know about HTTP or database entities

**3. Database Service Layer** (`database/db/service/`)
- Provides CRUD operations for database entities
- Maps between domain models and JPA entities using mappers
- Calls Spring Data repositories
- Handles database-specific exceptions
- **Does NOT** contain business logic

**4. Data Layer** (`database/db/repository/`)
- Spring Data JPA repositories
- Defines custom query methods when needed
- **Does NOT** contain any logic beyond queries

### Key Design Patterns

#### 1. Repository Pattern
Spring Data JPA repositories provide abstraction over data access:

```java
public interface FoodRepository extends JpaRepository<FoodDb, Long> {
    Optional<FoodDb> findByExtid(String extid);
    Optional<FoodDb> findByCode(String code);
    List<FoodDb> findByActiveOrderByNameAsc(ActiveEnum active);
}
```

#### 2. DTO Pattern
Separate DTOs for requests and responses prevent over-exposure of internal models:

```java
// Request DTO
public class RequestFoodCreate {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String category;
    // ... validation annotations
}

// Response DTO
public class ResponseFood {
    private String extid;
    private String code;
    private String name;
    private ResponseNutrition nutrition;
    // ... no sensitive fields
}
```

#### 3. Mapper Pattern
Separate mappers handle conversion between layers:

```java
@Component
public class FoodMapper {
    public Food toDomain(FoodDb entity) {
        // Entity → Domain conversion
    }

    public FoodDb toEntity(Food domain) {
        // Domain → Entity conversion
    }
}
```

#### 4. Builder Pattern
Lombok's `@Builder` pattern for clean object construction:

```java
Food food = Food.builder()
    .code("SPIN")
    .name("Spinach")
    .category("Greens")
    .nutrition(nutrition)
    .build();
```

### Domain Model vs JPA Entity

**Domain Models** (`common/domain/`):
- Pure Java POJOs
- No JPA annotations
- Used in service layer
- Represent business concepts

**JPA Entities** (`database/db/entity/`):
- Annotated with `@Entity`, `@Table`, etc.
- Map directly to database tables
- Used only in database layer
- Represent database schema

**Example:**

```java
// Domain Model
@Data
@SuperBuilder
public class Food extends BaseDomain {
    private String code;
    private String name;
    private Nutrition nutrition;  // Nested domain object
}

// JPA Entity
@Entity
@Table(name = "food")
public class FoodDb extends BaseDb {
    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "nutrition_id")
    private NutritionDb nutrition;  // JPA relationship
}
```

---

## Frontend Architecture

### Component Hierarchy

```
App.tsx (Router)
├── Login (Public Route)
├── ForgotUsername (Public Route)
├── ForgotPassword (Public Route)
├── ResetPassword (Public Route)
└── ProtectedRoute
    └── Layout (Navigation + Outlet)
        ├── Dashboard
        ├── BeginHere
        ├── MyStory
        ├── Foods
        ├── Nutrition
        ├── Salads
        ├── SaladBuilder
        ├── Mixtures
        ├── MakeMixture
        └── MixtureShop
```

### State Management Strategy

**1. Server State (TanStack Query)**
- All data fetched from backend API
- Automatic caching, refetching, and background updates
- Query invalidation on mutations

```typescript
const { data: foods, isLoading } = useQuery({
    queryKey: ['foods'],
    queryFn: () => foodApi.getAll()
});
```

**2. Client State (React Hooks)**
- Local component state via `useState`
- Form state via `react-hook-form`
- No global state management (Redux/Zustand) needed for current scope

**3. Authentication State (localStorage + Context)**
- JWT token stored in localStorage
- Token attached to all API requests via Axios interceptor
- Auth checks via `ProtectedRoute` component

### Routing Structure

React Router v7 with nested routes:

```typescript
<Routes>
    <Route path="/login" element={<Login />} />
    <Route path="/forgot-password" element={<ForgotPassword />} />
    <Route path="/reset-password" element={<ResetPassword />} />

    <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index element={<Dashboard />} />
        <Route path="salads" element={<Salads />} />
        <Route path="salad-builder/:extid?" element={<SaladBuilder />} />
        {/* ... more routes */}
    </Route>
</Routes>
```

### API Integration Pattern

All API calls centralized in `services/api.ts`:

```typescript
// API Client Configuration
const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_URL || '/api',
    headers: { 'Content-Type': 'application/json' }
});

// Request Interceptor (attach JWT)
apiClient.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Response Interceptor (handle 401/403)
apiClient.interceptors.response.use(
    response => response,
    error => {
        if (error.response?.status === 403 || error.response?.status === 401) {
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// API Functions
export const foodApi = {
    getAll: () => apiClient.get<Food[]>('/food'),
    getById: (extid: string) => apiClient.get<Food>(`/food/${extid}`),
    create: (food: FoodRequest) => apiClient.post<Food>('/food', food),
    update: (extid: string, food: FoodRequest) =>
        apiClient.put<Food>(`/food/${extid}`, food),
    delete: (extid: string) => apiClient.delete(`/food/${extid}`)
};
```

### Form Handling Pattern

Using React Hook Form + Zod validation:

```typescript
const schema = z.object({
    username: z.string().min(1, 'Username is required'),
    password: z.string().min(6, 'Password must be at least 6 characters')
});

const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(schema)
});

const onSubmit = async (data: LoginRequest) => {
    const response = await authApi.login(data);
    authHelpers.saveToken(response.data.token);
    navigate('/');
};
```

### Styling Approach

**Tailwind CSS Utility-First:**
- No CSS modules or styled-components
- All styles defined inline using Tailwind classes
- Custom configuration in `tailwind.config.js`
- Responsive design via Tailwind breakpoints

```tsx
<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
</div>
```

---

## Database Schema

### Entity Relationship Diagram

```
┌──────────────┐       ┌──────────────────┐       ┌─────────────┐
│   company    │       │    nutrition     │       │    users    │
├──────────────┤       ├──────────────────┤       ├─────────────┤
│ id (PK)      │       │ id (PK)          │       │ id (PK)     │
│ extid (UK)   │       │ extid (UK)       │       │ extid (UK)  │
│ code (UK)    │       │ code (UK)        │       │ username(UK)│
│ name         │       │ name             │       │ password    │
│ description  │       │ carbohydrate     │       │ email       │
│ active       │       │ fat              │       │ role        │
│ ...          │       │ protein          │       │ active      │
└──────────────┘       │ sugar            │       │ ...         │
                       │ fiber            │       └─────────────┘
                       │ vitaminD         │              │
                       │ vitaminE         │              │
                       │ calories         │              │
                       │ active           │              │
                       │ ...              │              │
                       └──────────────────┘              │
                                 │                       │
                                 │ FK                    │
                                 ▼                       │
                       ┌──────────────────┐              │
                       │      food        │              │
                       ├──────────────────┤              │
                       │ id (PK)          │              │
                       │ extid (UK)       │              │
                       │ code (UK)        │              │
                       │ name (UK)        │              │
                       │ category         │              │
                       │ subcategory      │              │
                       │ nutrition_id(FK) │              │
                       │ foundation       │              │
                       │ mixable          │              │
                       │ crunch           │              │
                       │ punch            │              │
                       │ sweet            │              │
                       │ savory           │              │
                       │ typical_serving_g│              │
                       │ active           │              │
                       │ ...              │              │
                       └──────────────────┘              │
                          │           │                  │
                 ┌────────┘           └────────┐         │
                 │                             │         │
                 ▼                             ▼         │
       ┌──────────────────┐          ┌──────────────────┤
       │ mixture_ingredient│          │ salad_food_ingr  │
       ├──────────────────┤          ├──────────────────┤
       │ id (PK)          │          │ id (PK)          │
       │ extid (UK)       │          │ extid (UK)       │
       │ mixture_id (FK)  │          │ salad_id (FK)    │
       │ food_extid (FK)  │          │ food_extid (FK)  │
       │ grams            │          │ grams            │
       │ active           │          │ active           │
       │ ...              │          │ ...              │
       └──────────────────┘          └──────────────────┘
                 ▲                             ▲
                 │                             │
                 │                             │
       ┌──────────────────┐          ┌──────────────────┐
       │     mixture      │          │      salad       │
       ├──────────────────┤          ├──────────────────┤
       │ id (PK)          │          │ id (PK)          │
       │ extid (UK)       │          │ extid (UK)       │
       │ name             │          │ name             │
       │ description      │          │ description      │
       │ user_extid (FK?) │◄─────────┤ user_extid (FK?) │
       │ active           │          │ active           │
       │ ...              │          │ ...              │
       └──────────────────┘          └──────────────────┘

                       ┌──────────────────────────┐
                       │  password_reset_token    │
                       ├──────────────────────────┤
                       │ id (PK)                  │
                       │ extid (UK)               │
                       │ user_extid (FK)  ────────┼──► users.extid
                       │ token (UK)               │
                       │ expires_at               │
                       │ used                     │
                       │ active                   │
                       │ ...                      │
                       └──────────────────────────┘
```

### Table Schemas

#### users
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    extid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,  -- BCrypt hashed
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted_at DATETIME,
    active INT DEFAULT 1
);
```

**Key Columns:**
- `extid`: External UUID for public API exposure (hides internal IDs)
- `password`: BCrypt-hashed password (never returned in API responses)
- `role`: User role (e.g., 'USER', 'ADMIN') for authorization
- `active`: Soft delete flag (1 = active, 0 = inactive)

#### nutrition
```sql
CREATE TABLE nutrition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    extid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    code VARCHAR(16) UNIQUE,
    name VARCHAR(32) NOT NULL UNIQUE,
    description VARCHAR(255),
    carbohydrate INT,      -- grams per 100g
    fat INT,               -- grams per 100g
    protein INT,           -- grams per 100g
    sugar INT,             -- grams per 100g
    fiber INT,             -- grams per 100g
    vitaminD INT,          -- IU per 100g
    vitaminE INT,          -- mg per 100g
    calories INT,          -- calculated: (carbs*4) + (protein*4) + (fat*9)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted_at DATETIME,
    active INT DEFAULT 1
);
```

**Note:** All nutritional values are per 100g for standardization.

#### food
```sql
CREATE TABLE food (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    extid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    code VARCHAR(16) UNIQUE,
    name VARCHAR(32) NOT NULL UNIQUE,
    category VARCHAR(32) NOT NULL,
    subcategory VARCHAR(32) NOT NULL,
    description VARCHAR(255),
    notes VARCHAR(1000),
    nutrition_id BIGINT,
    foundation BOOLEAN NOT NULL DEFAULT FALSE,
    mixable BOOLEAN NOT NULL DEFAULT FALSE,
    crunch TINYINT,        -- Flavor intensity (0-10)
    punch TINYINT,         -- Flavor intensity (0-10)
    sweet TINYINT,         -- Flavor intensity (0-10)
    savory TINYINT,        -- Flavor intensity (0-10)
    typical_serving_grams INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted_at DATETIME,
    active INT DEFAULT 1,
    FOREIGN KEY (nutrition_id) REFERENCES nutrition(id)
        ON DELETE SET NULL ON UPDATE CASCADE
);
```

**Key Columns:**
- `foundation`: Whether food can be a salad base (e.g., lettuce)
- `mixable`: Whether food can be mixed into custom mixtures
- `crunch/punch/sweet/savory`: Sensory flavor attributes (0-10 scale)
- `typical_serving_grams`: Standard serving size for UI suggestions

#### salad
```sql
CREATE TABLE salad (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    extid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    user_extid VARCHAR(36),  -- Nullable to allow system-created salads
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted_at DATETIME,
    active INT DEFAULT 1
);
```

#### salad_food_ingredient
```sql
CREATE TABLE salad_food_ingredient (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    extid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    salad_id BIGINT NOT NULL,
    food_extid VARCHAR(36) NOT NULL,
    grams INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted_at DATETIME,
    active INT DEFAULT 1,
    FOREIGN KEY (salad_id) REFERENCES salad(id) ON DELETE CASCADE,
    FOREIGN KEY (food_extid) REFERENCES food(extid) ON DELETE CASCADE
);
```

**Relationship:** Many-to-many between Salad and Food with quantity (grams)

#### mixture
```sql
CREATE TABLE mixture (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    extid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    user_extid VARCHAR(36),  -- Nullable
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted_at DATETIME,
    active INT DEFAULT 1
);
```

#### mixture_ingredient
```sql
CREATE TABLE mixture_ingredient (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    extid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    mixture_id BIGINT NOT NULL,
    food_extid VARCHAR(36) NOT NULL,
    grams INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted_at DATETIME,
    active INT DEFAULT 1,
    FOREIGN KEY (mixture_id) REFERENCES mixture(id) ON DELETE CASCADE,
    FOREIGN KEY (food_extid) REFERENCES food(extid) ON DELETE CASCADE
);
```

#### password_reset_token
```sql
CREATE TABLE password_reset_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    extid VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    user_extid VARCHAR(36) NOT NULL,
    token VARCHAR(36) NOT NULL UNIQUE,  -- UUID token
    expires_at DATETIME NOT NULL,       -- 1 hour expiration
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted_at DATETIME,
    active INT DEFAULT 1,
    FOREIGN KEY (user_extid) REFERENCES users(extid) ON DELETE CASCADE,
    INDEX idx_password_reset_token_token (token),
    INDEX idx_password_reset_token_user_extid (user_extid)
);
```

**Security Notes:**
- Token expires after 1 hour
- Token is single-use (marked as `used = TRUE` after consumption)
- All tokens deleted when user successfully resets password
- Cascade delete when user is deleted

### Common Patterns

**1. Base Columns (all tables):**
- `id`: Internal auto-increment primary key
- `extid`: External UUID for API exposure
- `created_at`: Timestamp of creation
- `updated_at`: Timestamp of last update
- `deleted_at`: Soft delete timestamp (NULL = active)
- `active`: Integer flag (1 = active, 0 = soft deleted)

**2. Soft Delete Pattern:**
Instead of physical deletion, rows are marked inactive:
```sql
UPDATE food SET active = 0, deleted_at = NOW() WHERE extid = ?
```

**3. UUID External IDs:**
All public APIs use `extid` instead of database IDs to prevent enumeration attacks and decouple public API from internal schema.

---

*Continued in next message...*
