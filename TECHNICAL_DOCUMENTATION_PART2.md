# CPSS Technical Documentation - Part 2

## Authentication & Security

### Authentication Flow

#### 1. User Registration Flow

```
┌────────┐                    ┌────────┐                 ┌──────────┐
│ Client │                    │  API   │                 │ Database │
└───┬────┘                    └───┬────┘                 └────┬─────┘
    │                             │                           │
    │ POST /api/auth/register     │                           │
    ├────────────────────────────>│                           │
    │ {username, password, email} │                           │
    │                             │                           │
    │                             │ Check username exists     │
    │                             ├──────────────────────────>│
    │                             │<──────────────────────────┤
    │                             │                           │
    │                             │ BCrypt hash password      │
    │                             │ (10 rounds)               │
    │                             │                           │
    │                             │ Save user                 │
    │                             ├──────────────────────────>│
    │                             │<──────────────────────────┤
    │                             │                           │
    │                             │ Generate JWT token        │
    │                             │ (24hr expiry)             │
    │                             │                           │
    │<────────────────────────────┤                           │
    │ {token, username, email,    │                           │
    │  role}                      │                           │
    │                             │                           │
    │ Store token in localStorage │                           │
    │                             │                           │
```

**Key Steps:**
1. Client submits registration form
2. Backend validates username uniqueness
3. Password is hashed using BCrypt (10 rounds, salted)
4. User record created with role='USER', active=1
5. JWT token generated with username as subject
6. Token and user info returned to client
7. Client stores token in localStorage

#### 2. Login Flow

```
┌────────┐                    ┌────────┐                 ┌──────────┐
│ Client │                    │  API   │                 │ Database │
└───┬────┘                    └───┬────┘                 └────┬─────┘
    │                             │                           │
    │ POST /api/auth/login        │                           │
    ├────────────────────────────>│                           │
    │ {username, password}        │                           │
    │                             │                           │
    │                             │ Load UserDetails          │
    │                             ├──────────────────────────>│
    │                             │<──────────────────────────┤
    │                             │                           │
    │                             │ Spring Security           │
    │                             │ AuthenticationManager     │
    │                             │ validates credentials     │
    │                             │ (BCrypt compare)          │
    │                             │                           │
    │<────────────────────────────┤                           │
    │ {token, username, email}    │                           │
    │                             │                           │
    │ Store token in localStorage │                           │
    │                             │                           │
```

**Key Steps:**
1. Client submits username and password
2. Spring Security's `AuthenticationManager` validates credentials
3. BCrypt compares submitted password with stored hash
4. If valid, JWT token generated
5. Token and user info returned
6. Client stores token for subsequent requests

#### 3. Authenticated Request Flow

```
┌────────┐                    ┌────────┐                 ┌──────────┐
│ Client │                    │  API   │                 │ Database │
└───┬────┘                    └───┬────┘                 └────┬─────┘
    │                             │                           │
    │ GET /api/food               │                           │
    │ Authorization: Bearer <JWT> │                           │
    ├────────────────────────────>│                           │
    │                             │                           │
    │                             │ JwtAuthenticationFilter   │
    │                             │ 1. Extract JWT from header│
    │                             │ 2. Validate signature     │
    │                             │ 3. Check expiration       │
    │                             │ 4. Extract username       │
    │                             │ 5. Load UserDetails       │
    │                             │ 6. Set SecurityContext    │
    │                             │                           │
    │                             │ Controller method         │
    │                             │ (user authenticated)      │
    │                             │                           │
    │                             │ Query database            │
    │                             ├──────────────────────────>│
    │                             │<──────────────────────────┤
    │                             │                           │
    │<────────────────────────────┤                           │
    │ Response data               │                           │
    │                             │                           │
```

**Key Components:**

**JwtAuthenticationFilter** (`security/JwtAuthenticationFilter.java`):
- Runs before every request (except public endpoints)
- Extracts JWT from Authorization header: `Bearer <token>`
- Validates token signature and expiration
- Loads user details from database
- Sets Spring Security authentication context

**JwtUtil** (`security/JwtUtil.java`):
- Generates tokens with HS256 algorithm
- Secret key from config (256+ bits)
- 24-hour expiration (configurable)
- Claims: username (subject), issued date, expiration

**CustomUserDetailsService** (`security/CustomUserDetailsService.java`):
- Implements Spring Security's `UserDetailsService`
- Loads user by username from database
- Returns `UserDetails` with username, password, and authorities

### Password Reset Flow

```
┌────────┐         ┌────────┐         ┌──────────┐         ┌───────┐
│ Client │         │  API   │         │ Database │         │ Email │
└───┬────┘         └───┬────┘         └────┬─────┘         └───┬───┘
    │                  │                   │                   │
    │ 1. Forgot Password                   │                   │
    │ POST /auth/forgot-password           │                   │
    ├─────────────────>│                   │                   │
    │ {email}          │                   │                   │
    │                  │                   │                   │
    │                  │ Find user by email│                   │
    │                  ├──────────────────>│                   │
    │                  │<──────────────────┤                   │
    │                  │                   │                   │
    │                  │ Generate UUID token                   │
    │                  │ Expires: NOW + 1hr│                   │
    │                  │                   │                   │
    │                  │ Save token        │                   │
    │                  ├──────────────────>│                   │
    │                  │                   │                   │
    │                  │ Send reset email  │                   │
    │                  │ (link: /reset-password?token=UUID)    │
    │                  ├───────────────────────────────────────>│
    │                  │                   │                   │
    │<─────────────────┤                   │                   │
    │ "Reset link sent"│                   │                   │
    │                  │                   │                   │
    │ 2. User clicks link in email         │                   │
    │ (navigates to /reset-password?token=UUID)                │
    │                  │                   │                   │
    │ 3. Submit new password               │                   │
    │ POST /auth/reset-password            │                   │
    ├─────────────────>│                   │                   │
    │ {token, password}│                   │                   │
    │                  │                   │                   │
    │                  │ Validate token    │                   │
    │                  ├──────────────────>│                   │
    │                  │ - Check exists    │                   │
    │                  │ - Check not expired                   │
    │                  │ - Check not used  │                   │
    │                  │<──────────────────┤                   │
    │                  │                   │                   │
    │                  │ BCrypt hash new pw│                   │
    │                  │                   │                   │
    │                  │ Update user.password                  │
    │                  ├──────────────────>│                   │
    │                  │                   │                   │
    │                  │ Mark token as used│                   │
    │                  ├──────────────────>│                   │
    │                  │                   │                   │
    │                  │ Delete other tokens                   │
    │                  │ for this user     │                   │
    │                  ├──────────────────>│                   │
    │                  │                   │                   │
    │<─────────────────┤                   │                   │
    │ "Password reset" │                   │                   │
    │                  │                   │                   │
```

**Security Features:**
1. **Token Expiration**: 1-hour validity window
2. **Single-Use Tokens**: Marked as `used=true` after consumption
3. **Token Cleanup**: All user tokens deleted after successful reset
4. **No User Enumeration**: Always returns generic success message
5. **Cascade Delete**: Tokens deleted if user is deleted

**Implementation:** `service/PasswordResetService.java`

### Spring Security Configuration

**File:** `config/SecurityConfig.java`

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http
        .csrf(AbstractHttpConfigurer::disable)  // Disabled for stateless JWT
        .cors(cors -> cors.configure(http))     // CORS enabled
        .authorizeHttpRequests(auth -> auth
            // Public: Static resources
            .requestMatchers("/", "/index.html", "/assets/**").permitAll()

            // Public: Auth endpoints
            .requestMatchers("/api/auth/**").permitAll()

            // Protected: All other API endpoints
            .requestMatchers("/api/**").authenticated()

            // Public: Swagger docs
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

            // Default: Authenticated
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

**Key Configurations:**
- **CSRF Disabled**: Not needed for stateless JWT authentication
- **Stateless Sessions**: No server-side session storage
- **JWT Filter**: Runs before Spring's default authentication filter
- **Public Endpoints**: `/api/auth/**`, static resources, Swagger
- **Protected Endpoints**: All `/api/**` (except auth)

### CORS Configuration

**File:** `config/WebConfig.java`

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins(allowedOrigins.split(","))  // From config
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
}
```

**Default Allowed Origins:** `http://localhost:5173,http://localhost:3000`

**Production:** Override via `cors.allowed.origins` environment variable

---

## API Reference

### Base URL

- **Development:** `http://localhost:8080/api`
- **Production:** `https://your-app.elasticbeanstalk.com/api`

### Authentication Endpoints

#### POST `/api/auth/register`
Register a new user account.

**Request:**
```json
{
  "username": "johndoe",
  "password": "securepass123",
  "email": "john@example.com"
}
```

**Response:** `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "johndoe",
  "email": "john@example.com",
  "role": "USER"
}
```

**Errors:**
- `409 Conflict`: Username already exists

---

#### POST `/api/auth/login`
Authenticate and receive JWT token.

**Request:**
```json
{
  "username": "johndoe",
  "password": "securepass123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "johndoe",
  "email": "john@example.com",
  "role": "USER"
}
```

**Errors:**
- `401 Unauthorized`: Invalid credentials

---

#### POST `/api/auth/forgot-username`
Send username reminder to email.

**Request:**
```json
{
  "email": "john@example.com"
}
```

**Response:** `200 OK`
```json
{
  "message": "If that email exists, username has been sent"
}
```

**Note:** Always returns success to prevent user enumeration.

---

#### POST `/api/auth/forgot-password`
Initiate password reset flow.

**Request:**
```json
{
  "email": "john@example.com"
}
```

**Response:** `200 OK`
```json
{
  "message": "If that email exists, a reset link has been sent"
}
```

**Email Content:** Reset link with 1-hour expiration token

---

#### POST `/api/auth/reset-password`
Reset password using token from email.

**Request:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "newsecurepass123"
}
```

**Response:** `200 OK`
```json
{
  "message": "Password has been reset successfully"
}
```

**Errors:**
- `400 Bad Request`: Invalid/expired/used token

---

### Food Endpoints

All require `Authorization: Bearer <token>` header.

#### GET `/api/food`
Get all foods.

**Response:** `200 OK`
```json
[
  {
    "extid": "f1234567-89ab-cdef-0123-456789abcdef",
    "code": "SPIN",
    "name": "Spinach",
    "category": "Greens",
    "subcategory": "Leafy",
    "description": "Fresh spinach leaves",
    "foundation": true,
    "mixable": false,
    "crunch": 2,
    "punch": 1,
    "sweet": 0,
    "savory": 3,
    "typicalServingGrams": 85,
    "nutrition": {
      "extid": "n1234...",
      "calories": 23,
      "carbohydrate": 4,
      "fat": 0,
      "protein": 3,
      "sugar": 0,
      "fiber": 2,
      "vitaminD": 0,
      "vitaminE": 2
    }
  }
]
```

---

#### GET `/api/food/{extid}`
Get specific food by external ID.

**Response:** `200 OK` (same as single food object above)

**Errors:**
- `404 Not Found`: Food doesn't exist

---

#### POST `/api/food`
Create new food.

**Request:**
```json
{
  "code": "KALE",
  "name": "Kale",
  "category": "Greens",
  "subcategory": "Leafy",
  "description": "Curly kale",
  "foundation": true,
  "mixable": false,
  "crunch": 4,
  "punch": 2,
  "sweet": 0,
  "savory": 3,
  "typicalServingGrams": 67,
  "nutritionExtid": "n1234..."
}
```

**Response:** `201 Created` (returns created food object)

---

#### PUT `/api/food/{extid}`
Update existing food.

**Request:** (partial update supported)
```json
{
  "description": "Updated description",
  "crunch": 5
}
```

**Response:** `200 OK` (returns updated food object)

---

#### DELETE `/api/food/{extid}`
Soft delete food.

**Response:** `204 No Content`

---

### Salad Endpoints

#### GET `/api/salad`
Get all salads.

**Response:** `200 OK`
```json
[
  {
    "extid": "s1234567-89ab-cdef-0123-456789abcdef",
    "name": "Green Power Salad",
    "description": "High-protein green salad",
    "userExtid": "u1234...",
    "foodIngredients": [
      {
        "extid": "i1234...",
        "foodExtid": "f1234...",
        "foodName": "Spinach",
        "grams": 100
      },
      {
        "extid": "i5678...",
        "foodExtid": "f5678...",
        "foodName": "Chicken Breast",
        "grams": 150
      }
    ],
    "totalNutrition": {
      "calories": 248,
      "carbohydrate": 4,
      "fat": 3,
      "protein": 51,
      "sugar": 0,
      "fiber": 2,
      "vitaminD": 0,
      "vitaminE": 2
    },
    "totalCrunch": 2,
    "totalPunch": 1,
    "totalSweet": 0,
    "totalSavory": 8,
    "totalGrams": 250,
    "active": "ACTIVE",
    "createdAt": "2026-02-13T10:30:00",
    "updatedAt": "2026-02-13T10:30:00"
  }
]
```

**Note:** `totalNutrition` and flavor totals are calculated server-side by scaling nutrition per 100g to actual grams.

---

#### POST `/api/salad`
Create new salad.

**Request:**
```json
{
  "name": "My Custom Salad",
  "description": "Delicious and nutritious",
  "foodIngredients": [
    {
      "foodExtid": "f1234...",
      "grams": 100
    },
    {
      "foodExtid": "f5678...",
      "grams": 50
    }
  ]
}
```

**Response:** `201 Created` (returns created salad with totals)

**Note:** `userExtid` is automatically set from authenticated user.

---

#### PUT `/api/salad/{extid}`
Update salad.

**Request:**
```json
{
  "name": "Updated Salad Name",
  "foodIngredients": [
    {
      "foodExtid": "f1234...",
      "grams": 150
    }
  ]
}
```

**Response:** `200 OK`

---

#### DELETE `/api/salad/{extid}`
Delete salad.

**Response:** `204 No Content`

---

### Mixture Endpoints

Similar to Salad endpoints but for mixtures:
- `GET /api/mixture`
- `GET /api/mixture/{extid}`
- `POST /api/mixture`
- `PUT /api/mixture/{extid}`
- `DELETE /api/mixture/{extid}`

**Structure:** Same as salads (ingredients with grams, calculated totals)

---

### Nutrition Endpoints

- `GET /api/nutrition` - List all nutrition profiles
- `GET /api/nutrition/{extid}` - Get specific nutrition
- `POST /api/nutrition` - Create nutrition profile
- `PUT /api/nutrition/{extid}` - Update nutrition
- `DELETE /api/nutrition/{extid}` - Delete nutrition

---

### Company Endpoints

- `GET /api/company` - List companies
- `GET /api/company/{extid}` - Get company
- `POST /api/company` - Create company
- `PUT /api/company/{extid}` - Update company
- `DELETE /api/company/{extid}` - Delete company

---

## Data Flow

### Salad Creation Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        SALAD CREATION FLOW                      │
└─────────────────────────────────────────────────────────────────┘

1. USER INPUT (Frontend)
   └─> User selects foods and quantities in SaladBuilder.tsx
       - Spinach: 100g
       - Chicken: 150g
       - Tomatoes: 50g

2. CLIENT VALIDATION
   └─> Form validation (react-hook-form + zod)
       - Name required
       - At least one ingredient
       - Grams > 0

3. API REQUEST
   └─> POST /api/salad
       Authorization: Bearer <JWT>
       Body: { name, description, foodIngredients: [...] }

4. BACKEND AUTHENTICATION (JwtAuthenticationFilter)
   └─> Extract JWT → Validate → Load user → Set SecurityContext

5. CONTROLLER LAYER (SaladController)
   └─> Convert RequestDTO → Domain Model
       └─> Extract username from Authentication
       └─> Set userExtid on Salad domain

6. SERVICE LAYER (SaladService)
   └─> Business logic validation
       └─> Check food exists for each ingredient
       └─> Validate user permissions (if needed)

7. DATABASE SERVICE LAYER (SaladDbService)
   └─> Convert Domain → Entity
       └─> Map Salad → SaladDb
       └─> Map SaladFoodIngredient → SaladFoodIngredientDb
   └─> Save to database (transactional)
       └─> INSERT INTO salad (...)
       └─> INSERT INTO salad_food_ingredient (...) -- for each
   └─> Load relationships (foods, nutrition)
   └─> Convert Entity → Domain (with loaded data)

8. RESPONSE CALCULATION (SaladController)
   └─> For each ingredient:
       └─> Load food with nutrition
       └─> Scale nutrition from per-100g to actual grams
           - Spinach nutrition * (100/100) = base
           - Chicken nutrition * (150/100) = 1.5x
           - Tomato nutrition * (50/100) = 0.5x
       └─> Sum all scaled values
   └─> Calculate flavor totals (same scaling)
   └─> Build ResponseSalad with totals

9. API RESPONSE
   └─> Return ResponseSalad JSON (201 Created)

10. CLIENT UPDATE (Frontend)
    └─> TanStack Query invalidates ['salads'] cache
    └─> Navigate to salads list
    └─> Display new salad with nutrition totals
```

### Nutrition Calculation Formula

**Per Ingredient:**
```
actualNutrient = (nutrientPer100g × actualGrams) / 100

Example (Spinach 100g):
  carbs = (4g × 100) / 100 = 4g
  protein = (3g × 100) / 100 = 3g

Example (Chicken 150g):
  protein = (31g × 150) / 100 = 46.5g ≈ 47g
```

**Total Calories:**
```
calories = (totalCarbs × 4) + (totalProtein × 4) + (totalFat × 9)
```

**Implementation:** `web/controller/SaladController.java` (SaladConverter.calculateTotalNutrition)

---

## Deployment Architecture

### AWS Elastic Beanstalk Deployment

**Environment:**
- **Platform:** Java 21 (Corretto)
- **Instance Type:** t2.micro (or configured)
- **Load Balancer:** Application Load Balancer (if multi-instance)
- **Port:** 8080 (Spring Boot default)

**Deployment Package:**
- Single JAR file: `build/libs/cpss-0.0.2-SNAPSHOT.jar`
- Contains:
  - Compiled Java classes
  - Spring Boot dependencies
  - Static frontend (built React app in `src/main/resources/static/`)
  - Database migrations (Liquibase changelogs)

**Environment Variables (Elastic Beanstalk):**
```
RDS_HOSTNAME=<rds-endpoint>.rds.amazonaws.com
RDS_PORT=3306
RDS_DB_NAME=cpss
RDS_USERNAME=admin
RDS_PASSWORD=<secure-password>
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<gmail-account>
MAIL_PASSWORD=<app-specific-password>
MAIL_FROM=noreply@cpss.com
FRONTEND_URL=https://your-app.elasticbeanstalk.com
cors.allowed.origins=https://your-app.elasticbeanstalk.com
```

### AWS RDS MySQL Configuration

**Instance Specs:**
- **Engine:** MySQL 8.0+
- **Instance Class:** db.t3.micro (free tier) or larger
- **Storage:** 20GB GP2 SSD (auto-scaling enabled)
- **Multi-AZ:** False (dev), True (production)
- **Backup:** Automated daily snapshots (7-day retention)

**Security Group:**
- **Inbound:** Port 3306 from Elastic Beanstalk security group
- **Outbound:** All traffic

**Database Setup:**
1. Create RDS instance via AWS Console
2. Note endpoint, port, master username/password
3. Configure security group to allow EB access
4. Liquibase auto-runs migrations on app startup

### Email Configuration (Gmail SMTP)

**Gmail Account Setup:**
1. Create Gmail account (or use existing)
2. Enable 2-factor authentication
3. Generate App-Specific Password (not regular password)
4. Configure environment variables:
   - `MAIL_USERNAME`: Gmail address
   - `MAIL_PASSWORD`: App-specific password
   - `MAIL_FROM`: Sender email (can be same as username)

**SMTP Settings:**
- **Host:** smtp.gmail.com
- **Port:** 587 (STARTTLS)
- **Auth:** Required
- **TLS:** Required

**Alternative:** Use AWS SES (Simple Email Service) for production

### Deployment Process

See [Build & Deployment Process](#build--deployment-process) section.

---

*Continued in Part 3...*
