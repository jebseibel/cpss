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
- **Food** - Core entity: category, subcategory, the crunch/punch/sweet/savory flavor
  quadrant, `typicalServingGrams`, and the `foundation` / `mixable` capability flags
- **Nutrition** - Nutritional data per 100g (carbs, fat, protein, sugar, fiber, vitamins D and E)
- **Salad** / **SaladFoodIngredient** - A salad and its weighted food ingredients (grams)
- **Mixture** / **MixtureIngredient** - A dry blend and its weighted food ingredients
- **PasswordResetToken** - Single-use, time-boxed reset tokens

### Relationships
- Food has one-to-one with Nutrition
- Salad and Mixture each own a list of ingredients referencing Food by extid, with grams
- All entities inherit from BaseDb (id, extid, timestamps, soft delete)

> **Flavor and Serving no longer exist as entities.** Flavor was folded into `Food` as four
> integer columns (crunch, punch, sweet, savory) — they are intrinsic properties of a food, not
> a separately-identified thing. The `Serving` table was dropped in changeset
> `014-drop-serving-table.yaml` and replaced by the single `typicalServingGrams` field, once
> per-100g normalization made the unit conversions unnecessary. See `_archive/database/DOMAIN_MODEL.md`.

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
- Standard CRUD. **The only paginated endpoint** — `GET` returns a `Page` with a default size
  of 20, a maximum page size, and a sort-field whitelist enforced in `CompanyService`

**FoodController** (`/api/food`)
- CRUD for foods with nested nutrition data. Returns an unbounded list

**SaladController** (`/api/salad`)
- CRUD plus `GET /user/{userExtid}`. Responses carry aggregated nutrition and the flavor
  quadrant, computed by `NutritionCalculator`

**MixtureController** (`/api/mixture`)
- CRUD for dry mixtures. Aggregated nutrition only — mixtures carry no flavor profile

**NutritionController** (`/api/nutrition`)
- CRUD for nutrition records

> **Pagination is Company-only.** Food, Salad, Mixture, and Nutrition all return unbounded
> `List<...>` responses. Earlier revisions of this document claimed pagination was project-wide
> and the technical documentation claimed there was none at all; neither was accurate.

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
   ↳ LoginRateLimitFilter runs first: 8 consecutive failures per IP+username
     returns 429 with Retry-After for 15 minutes
2. Client stores token (localStorage)
3. Client sends: Authorization: Bearer <token>
4. JwtAuthenticationFilter validates token
5. Request proceeds to protected endpoints
```

`LoginRateLimitFilter` is registered ahead of `JwtAuthenticationFilter` so a locked-out caller
is rejected before any BCrypt hashing happens. It also throttles `/forgot-password` and
`/forgot-username`, which would otherwise be an email-bombing vector against a third party.

## Key Features
- Soft deletes (deletedAt timestamp, active status)
- Pagination on Company only (see the note in the REST API layer above)
- Auto-code generation for entities
- Aggregated salad nutrition and flavor, computed in `NutritionCalculator`
- Foundation-ingredient validation: every salad must contain at least one `foundation=true` food
- Login rate limiting on the public auth endpoints
- Comprehensive validation and error handling
- Swagger/OpenAPI documentation

## Known Limitations
- **Nutrition uses integer arithmetic end to end**, so small quantities truncate toward zero —
  a food with 5g carbohydrate per 100g contributes 0 at a 10g portion. Documented in
  `_archive/database/DOMAIN_MODEL.md` and asserted in `NutritionCalculatorTest`.
- **JWTs cannot be revoked before their 24-hour expiry**, and a password reset does not
  invalidate outstanding tokens.
- **Toxicity and diabetes warnings are not implemented** (required per `CLAUDE.md`).
- **Mixtures cannot yet be added to salads** — designed, not built.

For the reasoning behind these, and the alternatives that were rejected, see
`DESIGN_DECISIONS.md`.
