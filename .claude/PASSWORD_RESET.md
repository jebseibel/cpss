# Password Reset Feature Documentation

## Overview

The password reset feature allows users to securely reset their password if they've forgotten it. The system uses time-limited, single-use tokens sent via email to verify the user's identity.

## Architecture

### Frontend Flow

1. **Forgot Password Page** (`frontend/src/pages/ForgotPassword.tsx`)
   - User enters their email address
   - Calls `/auth/forgot-password` endpoint
   - Shows generic success message: "If that email exists, a reset link has been sent"
   - Does not reveal whether the email is registered (security best practice)

2. **Reset Password Page** (`frontend/src/pages/ResetPassword.tsx`)
   - User clicks the reset link from their email: `/reset-password?token=<uuid>`
   - Extracts the token from the URL query parameter
   - User enters new password and confirmation password
   - Client-side validation:
     - Passwords must match
     - Minimum 6 characters
   - Calls `/auth/reset-password` endpoint with token and new password
   - Shows success message and redirects to login after 2 seconds

### Backend Flow

#### 1. Initiating Password Reset

**Endpoint:** `POST /auth/forgot-password`

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Process:**
- Searches for user by email in the database
- If user found:
  - Generates a UUID token
  - Sets expiration to 1 hour from now
  - Saves token to `password_reset_token` table with `used = false`
  - Sends email with reset link: `{FRONTEND_URL}/reset-password?token={token}`
- If user not found:
  - Silently returns success (doesn't reveal if email exists)
- Logs the action for debugging

**Response:**
```json
{
  "message": "If that email exists, a reset link has been sent"
}
```

#### 2. Resetting Password

**Endpoint:** `POST /auth/reset-password`

**Request:**
```json
{
  "token": "uuid-token-from-email",
  "newPassword": "user_new_password"
}
```

**Process:**
1. **Token Validation** (`validateResetToken()`)
   - Retrieves token from database
   - Checks if token is expired (compare current time with `expires_at`)
   - Checks if token has already been used (`used = true`)
   - Verifies user still exists
   - Throws exception if any check fails

2. **Password Update**
   - Encodes new password using bcrypt
   - Updates user's password in `users` table
   - Sets `updated_at` timestamp

3. **Token Cleanup**
   - Marks the token as used (`used = true`)
   - Deletes all other reset tokens for this user (prevents old token reuse)

**Response:**
```json
{
  "message": "Password has been reset successfully"
}
```

## Database Schema

### password_reset_token Table

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | bigint | PRIMARY KEY, auto-increment | Internal database ID |
| extid | varchar(36) | UNIQUE, NOT NULL | UUID for the token |
| user_extid | varchar(36) | NOT NULL, FK to users | References user by extid |
| token | varchar(36) | UNIQUE, NOT NULL | The actual reset token sent in email |
| expires_at | datetime | NOT NULL | Token expiration time (1 hour from creation) |
| used | boolean | NOT NULL, default false | Whether token has been used |
| created_at | datetime | NOT NULL, default CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | datetime | NULL | Last update timestamp |
| deleted_at | datetime | NULL | Soft delete timestamp |
| active | int | default 1 | Active status |

### Indexes

- `idx_password_reset_token_token` on `token` column (fast lookup by token)
- `idx_password_reset_token_user_extid` on `user_extid` column (cleanup queries)
- Foreign key constraint to `users` table with CASCADE on delete

## Services

### EmailService

Handles all email communication. Located at: `src/main/java/com/seibel/cpss/service/EmailService.java`

**Configuration:**
- Host: `${MAIL_HOST:smtp.gmail.com}`
- Port: `${MAIL_PORT:587}`
- Username: `${MAIL_USERNAME}` (required from environment)
- Password: `${MAIL_PASSWORD}` (required from environment)
- From Address: `${MAIL_FROM:noreply@cpss.com}`
- Protocol: SMTP with TLS/STARTTLS

**Methods:**

1. `sendUsernameReminder(String email, String username)`
   - Sends email with the user's username
   - Subject: "Your CPSS Username"

2. `sendPasswordResetLink(String email, String resetLink)`
   - Sends email with password reset link
   - Subject: "Password Reset Request"
   - Includes note that link expires in 1 hour
   - Advises user to ignore if they didn't request it

### PasswordResetService

Core business logic for password reset. Located at: `src/main/java/com/seibel/cpss/service/PasswordResetService.java`

**Configuration:**
- Frontend URL: `${FRONTEND_URL:http://localhost:5173}`

**Methods:**

1. `initiatePasswordReset(String email)`
   - Creates reset token with 1-hour expiration
   - Saves to database
   - Sends email with reset link
   - Silently fails if email doesn't exist

2. `validateResetToken(String token)`
   - Verifies token exists
   - Checks expiration
   - Checks if already used
   - Validates user still exists
   - Throws exception if invalid

3. `resetPassword(String token, String newPassword)`
   - Validates token
   - Updates user password (bcrypt encoded)
   - Marks token as used
   - Deletes all other tokens for the user
   - Throws exception if validation fails

## Security Considerations

### Implemented

1. **Email Enumeration Protection**
   - Both forgot-username and forgot-password return generic success messages
   - Doesn't reveal whether email is registered in the system

2. **Token Security**
   - Tokens are UUIDs (cryptographically random)
   - Stored in database with hash/uniqueness constraints
   - Each token is single-use (`used` flag)

3. **Expiration**
   - Tokens expire after 1 hour
   - Validated before use

4. **Password Hashing**
   - New passwords are encoded with bcrypt
   - Never stored in plaintext

5. **Token Cleanup**
   - Old tokens deleted after successful reset
   - Prevents token reuse from old emails

6. **User Verification**
   - User must exist and be active
   - Validates at multiple stages

### Potential Enhancements

1. Rate limiting on password reset requests (prevent spam)
2. Email verification to confirm user owns the email
3. Password strength requirements beyond minimum length
4. Notification email when password is successfully reset
5. Invalidate all active sessions after password reset
6. Log password reset attempts for security audit trail

## Configuration

### Environment Variables

Required for production:
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@yourapp.com
FRONTEND_URL=https://yourapp.com
```

### Local Development

The system uses environment variable defaults:
- SMTP: Gmail (smtp.gmail.com:587)
- Frontend: http://localhost:5173
- From email: noreply@cpss.com

Note: You'll need valid Gmail credentials with "App Password" enabled for testing locally.

## API Endpoints

### 1. Forgot Username
```
POST /auth/forgot-username
Content-Type: application/json

{
  "email": "user@example.com"
}

Response: 200 OK
{
  "message": "If that email exists, username has been sent"
}
```

### 2. Forgot Password
```
POST /auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}

Response: 200 OK
{
  "message": "If that email exists, a reset link has been sent"
}
```

### 3. Reset Password
```
POST /auth/reset-password
Content-Type: application/json

{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "new_secure_password"
}

Response: 200 OK
{
  "message": "Password has been reset successfully"
}

Response: 400 Bad Request
{
  "message": "Failed to reset password: <reason>"
}
```

## Testing Password Reset Locally

1. **Set up email (Gmail example):**
   - Enable 2-factor authentication
   - Create an "App Password"
   - Set environment variables with credentials

2. **Trigger forgot password:**
   ```bash
   curl -X POST http://localhost:8080/auth/forgot-password \
     -H "Content-Type: application/json" \
     -d '{"email":"your-email@gmail.com"}'
   ```

3. **Check email for reset link**

4. **Use the token to reset:**
   ```bash
   curl -X POST http://localhost:8080/auth/reset-password \
     -H "Content-Type: application/json" \
     -d '{"token":"<token-from-email>","newPassword":"new_password"}'
   ```

5. **Log in with new password**

## Files Involved

### Frontend
- `frontend/src/pages/ForgotPassword.tsx` - Forgot password form
- `frontend/src/pages/ForgotUsername.tsx` - Forgot username form
- `frontend/src/pages/ResetPassword.tsx` - Reset password form
- `frontend/src/services/api.ts` - API integration (three new methods)

### Backend
- `src/main/java/com/seibel/cpss/service/EmailService.java` - Email handling
- `src/main/java/com/seibel/cpss/service/PasswordResetService.java` - Password reset logic
- `src/main/java/com/seibel/cpss/web/controller/AuthController.java` - API endpoints
- `src/main/java/com/seibel/cpss/common/domain/PasswordResetToken.java` - Domain model
- `src/main/java/com/seibel/cpss/database/db/entity/PasswordResetTokenDb.java` - Database entity
- `src/main/java/com/seibel/cpss/database/db/service/PasswordResetTokenDbService.java` - Database service
- `src/main/java/com/seibel/cpss/database/db/repository/PasswordResetTokenRepository.java` - Database repository
- `src/main/java/com/seibel/cpss/web/request/RequestForgotPassword.java` - Request DTO
- `src/main/java/com/seibel/cpss/web/request/RequestForgotUsername.java` - Request DTO
- `src/main/java/com/seibel/cpss/web/request/RequestResetPassword.java` - Request DTO
- `src/main/java/com/seibel/cpss/web/response/ResponseMessage.java` - Response DTO

### Database
- `src/main/resources/db/changelog/changes/020-create-password-reset-token.yaml` - Liquibase migration

### Configuration
- `src/main/resources/application.yml` - Email and app configuration
