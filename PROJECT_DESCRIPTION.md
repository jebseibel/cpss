# CPSS — Custom Prepared Salad System

**Project description · demo project**
Version 0.0.2-SNAPSHOT · Written 2026-08-12

---

## What this is

CPSS is a full-stack web application for composing custom salads and dry food mixtures from a
catalog of individual foods, with nutrition and flavor totals calculated from the ingredients
as you build.

**This is a demonstration project.** It is not a product, has no users, and is not operated as
a service. It exists to exercise a complete layered Spring Boot + React stack end to end —
schema migrations, JWT authentication, a real domain calculation, and a working UI — on a
domain small enough to reason about but not trivial. Where a shortcut was taken because this
is a demo, this document says so.

## The problem it models

Nutrition labels are per-package and serving sizes are incommensurable. "One serving of
romaine" and "one serving of feta" cannot be added together without first converting both to a
common unit, so a bowl assembled from a dozen ingredients has no readable nutrition figure
unless something computes it.

CPSS normalizes every food to **per-100g** values and derives everything by scaling:

```
value = (valuePer100g × grams) / 100
calories = (carbs × 4) + (protein × 4) + (fat × 9)     // Atwater factors, never stored
```

The same treatment applies to the four flavor dimensions — **crunch, punch, sweet, savory** —
so a salad gets an aggregate flavor profile alongside its macros, and the user can see that a
bowl is heavy on crunch and short on punch before eating it.

## What it does

| Capability | Detail |
|---|---|
| **Food catalog** | ~166 foods across 15 categories (vegetables, cheese, nuts, herbs, oils, vinegars, protein, …), each with nutrition per 100g and a flavor quadrant |
| **Salad builder** | Pick foods, assign grams, see nutrition and flavor totals recalculate; category and foundation filters on the ingredient picker |
| **Mixture creator** | Dry blends of foods with aggregated nutrition (no flavor profile — mixtures are ingredients, not meals) |
| **Nutrition analysis** | Macros (carbs, protein, fat), sugar, fiber, vitamins D and E; calories derived from macros |
| **Foundation rule** | Every salad must contain at least one food flagged `foundation = true` — enforced server-side, not just in the UI |
| **Authentication** | JWT login and registration, BCrypt password storage, email-based password reset and username reminder, per-IP+username login rate limiting |

## Architecture at a glance

```
React 19 SPA  ──HTTP/JSON──▶  Spring Boot 3.5 REST API  ──JDBC──▶  MySQL 8
(Vite, TS,                    (Java 21, Spring Security,           (Liquibase
 Tailwind 4,                   JWT, JPA/Hibernate)                  migrations,
 TanStack Query)                        │                           CSV seed data)
       │                                ▼
  localStorage                    SMTP (password reset,
   (JWT token)                     username reminder)
```

Each REST resource is a nine-file vertical slice:

```
Domain POJO → Request/Response DTOs → JPA Entity → Mapper → Repository
           → DbService → Business Service → Controller + Converter
           → Liquibase changeset
```

Two deliberate separations run through it:

- **Domain models are not JPA entities.** Services work with inert POJOs, so Hibernate
  semantics — lazy proxies, detached-entity exceptions, accidental queries during
  serialization — cannot leak past the persistence boundary.
- **`extid` (UUID) on the wire, numeric `id` internally.** No API path or payload exposes a
  sequential key, so records are not enumerable. This is obfuscation, not authorization.

Both cost real boilerplate. The reasoning and the price are argued out in
[`DESIGN_DECISIONS.md`](DESIGN_DECISIONS.md).

## Tech stack

**Backend** — Java 21 · Spring Boot 3.5.7 · Spring Security · JJWT · Spring Data JPA ·
ModelMapper · Liquibase 2.2 · Gradle · Swagger/OpenAPI
**Frontend** — React 19 · TypeScript 5.9 · Vite 7 · Tailwind CSS 4 · TanStack Query 5 ·
React Router 7
**Data** — MySQL 8 (AWS RDS in the deployed configuration); 20 structural changesets plus
CSV-loaded seed catalog
**Deployment** — AWS Elastic Beanstalk, single JAR with the built frontend embedded

## Repository layout

```
src/main/java/com/seibel/cpss/
  common/domain/        Plain domain POJOs (BaseDomain: id, extid, timestamps, active)
  common/exception/     ResourceNotFound(404) · Validation(400) · AlreadyExists(409) · Service(500)
  config/               SecurityConfig, WebConfig (CORS)
  security/             JwtUtil, JwtAuthenticationFilter, LoginRateLimitFilter
  database/db/          Entities, mappers, repositories, DbServices
  service/              Business services · NutritionCalculator
  web/controller/       Auth · Company · Food · Salad · Mixture · Nutrition
src/main/resources/db/
  changelog/changes/    001–020 structural, 100-load-csv-data.yaml
  data/                 Food and nutrition catalog as CSV
frontend/src/
  pages/                14 pages — Login, Dashboard, Foods, SaladBuilder, Salads,
                        Mixtures, MakeMixture, MixtureShop, Nutrition, BeginHere,
                        MyStory, ForgotPassword, ForgotUsername, ResetPassword
  services/api.ts       API client
```

## Constraints that shaped the build

- **No modals in the frontend.** Standing project rule. Create, edit, and confirm flows are
  full pages or inline sections — more routes, but linkable and mobile-workable.
- **Soft deletes everywhere.** `deletedAt` + `active` on every record; no hard-delete or purge
  path exists.
- **Calories are never stored.** Derived at response time so macros and calories cannot drift
  out of agreement.
- **The layered scaffold is generated, not copied.** The nine-file pattern lives in checked-in
  skills under `.claude/skills/` rather than as a convention, because copying an existing
  entity carries its accidents along with its structure.

## Known limitations

These are real and unfixed, worst first. This is a demo, and the list is part of the artifact
rather than something to be discovered later.

1. **Toxicity and diabetes warnings are not implemented**, though they are a stated project
   requirement. The app displays sugar content with no context. This is the one gap where
   being incomplete could actually mislead someone.
2. **JWTs cannot be revoked before their 24-hour expiry.** A password reset does not
   invalidate outstanding tokens, so "log out everywhere" is not implementable as built. The
   fix is short-lived access tokens plus a refresh flow, which is not built.
3. **Nutrition uses integer arithmetic end to end**, so small portions truncate toward zero — a
   food with 5g carbohydrate per 100g contributes 0 at a 10g portion. Asserted in
   `NutritionCalculatorTest`, so a future `BigDecimal` migration fails visibly rather than
   silently changing reported numbers.
4. **No notification email when a password changes**, so a successful account takeover is
   silent.
5. **Pagination exists on `Company` only.** Food, Salad, Mixture, and Nutrition return
   unbounded lists. Currently harmless at ~166 foods; it is a scaling cliff, not a live
   problem.
6. **Login rate limiting is in-memory and per-instance.** It resets on restart and does not
   coordinate across instances — adequate for a single-instance deployment, needs Redis
   otherwise.
7. **Mixtures cannot be added to salads.** Designed, not built; the open question is how to
   handle the second scaling denominator.

## Running it

```bash
./gradlew bootRun                 # backend  → http://localhost:8080
cd frontend && npm run dev        # frontend → http://localhost:5173
```

Create a local `.env` for database and SMTP credentials — it is git-ignored and no real
credentials are committed. API docs at `/swagger-ui/index.html`. The database can be reset via
the local n8n webhook (`GET http://localhost:5678/webhook/clear-cpss-db`) or by re-running
migrations with `./gradlew update`.

Test suite: **125 tests, 0 failures** as of 2026-08-12.

## Further reading

| Document | What it covers |
|---|---|
| [`DESIGN_DECISIONS.md`](DESIGN_DECISIONS.md) | Every significant decision, the alternative rejected, and what it cost |
| [`.claude/ARCHITECTURE.md`](.claude/ARCHITECTURE.md) | Entity-by-entity and endpoint-by-endpoint reference |
| [`documents/CPSS_TECHNICAL_DOCUMENTATION.md`](documents/CPSS_TECHNICAL_DOCUMENTATION.md) | Full technical manual (3 parts: architecture · security & API · dev/ops) |
| [`.claude/skills/skills-reference.md`](.claude/skills/skills-reference.md) | The code-generation skills and why they are forked locally |
