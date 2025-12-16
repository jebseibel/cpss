# Gradle Build & Deployment

## Build Process Overview

This project uses Gradle to build a Spring Boot application with an integrated React frontend. 
The build process automatically compiles both backend and frontend, packaging everything into a single executable JAR.

## Automated Frontend Build

The Gradle build includes custom tasks that automatically:

1. **Install frontend dependencies** (`npmInstall`)
2. **Build React app with Vite** (`npmBuild`)
3. **Clean static resources** (`cleanStatic`)
4. **Copy frontend build to Spring Boot** (`copyFrontend`)

These tasks run automatically when you execute `./gradlew buildDeployment`.

## Build Commands

### Regular Build (Development/Testing)
```bash
./gradlew build
```
- Compiles Java source code
- Packages backend-only JAR
- Runs tests
- **Does NOT build frontend** (faster for debugging)

### Deployment Build (Production)
```bash
./gradlew clean buildDeployment
```
- Cleans previous builds
- Builds React frontend with Vite
- Compiles Java source code
- Packages everything into JAR with frontend included
- **Use this for deployment**

### Build Without Tests
```bash
./gradlew clean buildDeployment -x test
```

### Frontend Only
```bash
./gradlew copyFrontend
```
Useful when you only changed frontend code.

### Available Frontend Tasks
```bash
./gradlew tasks --group frontend
```
Shows:
- `npmInstall` - Install frontend dependencies
- `npmBuild` - Build frontend with Vite
- `copyFrontend` - Copy frontend build to static resources

## Build Output

After running `./gradlew build`:

### JAR Location
```
build/libs/cpss-0.0.2-SNAPSHOT.jar
```

### JAR Contents
- Spring Boot application (Java compiled classes)
- React frontend (in `BOOT-INF/classes/static/`)
- All dependencies
- Database migrations (Liquibase changelogs)
- Application configuration

### What's Included in Static Resources
```
static/
├── index.html          (React app entry point)
├── vite.svg           (React logo)
└── assets/
    ├── index-*.js     (React app JavaScript bundle)
    └── index-*.css    (Tailwind CSS styles)
```

## How It Works

1. **`buildDeployment` task** depends on `copyFrontend` and `build`
2. **`copyFrontend` task** depends on `npmBuild` and `cleanStatic`
3. **`npmBuild` task** depends on `npmInstall`

This creates a dependency chain:
```
buildDeployment → [copyFrontend → [npmBuild → npmInstall, cleanStatic], build]
```

## Frontend Build Process

```bash
# Step 1: Clean old static files
rm -rf src/main/resources/static/*

# Step 2: Install dependencies
cd frontend && npm install

# Step 3: Build with Vite
npm run build
# Output: frontend/dist/

# Step 4: Copy to Spring Boot
cp -r frontend/dist/* src/main/resources/static/
```

## Deployment Workflow

1. Make code changes (backend or frontend)
2. Run `./gradlew clean buildDeployment`
3. Deploy `build/libs/cpss-0.0.2-SNAPSHOT.jar`
4. The JAR contains everything needed to run the application

## Development Workflow

1. Make code changes (backend only)
2. Run `./gradlew build` (faster, no frontend build)
3. Test locally with `./gradlew bootRun`

## Notes

- Use `./gradlew build` for fast backend-only builds during development
- Use `./gradlew buildDeployment` when you need to include the frontend
- No separate web server needed - Spring Boot serves the React app
- Both frontend and API run on the same port (8080)
- React routes are handled by React Router
- API endpoints are prefixed with `/api/`
