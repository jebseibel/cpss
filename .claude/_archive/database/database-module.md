# Database Layer

## What It Is

The **database** layer is the persistence tier: JPA entities, repositories, database services,
mappers, and Liquibase migrations for the CPSS application. It manages all database
interactions with MySQL.

> **Verified 2026-08-12** against `src/main/java/com/seibel/cpss/database/` and
> `src/main/resources/db/changelog/changes/`.

> **CPSS is a single-module Gradle project.** This document originally described a
> multi-module layout (`database/src/main/java/...`, a separate `:datafetcher` module). CPSS
> has one module — everything lives under `src/main/java/com/seibel/cpss/`, and the "layer"
> boundary is a package boundary, not a module boundary. Paths below reflect that.

**Rules**
- Table ID fields (primary keys) must never leave the database layer in any form.
- All tables must have an `extid` field, used for all communication into and out of the
  database layer.
- Do not add INDEX fields on tables unless they are `extid` or `id` fields. We don't
  anticipate enough data to need them.

## Why

Following industry-standard architecture:
- **Centralized Data Layer** — single source of truth for all database entities
- **Repository Pattern** — abstracts database operations
- **Database Versioning** — Liquibase manages schema evolution
- **Separation of Concerns** — database logic separate from business logic

Domain models are deliberately **not** JPA entities; mappers translate at the boundary so
Hibernate semantics don't leak into the service layer. See `DESIGN_DECISIONS.md` §1, which
also documents the mapper bug this separation cost (`SaladMapper.toModel()` silently returned
empty `foodIngredients`).

## What It Does

### 1. JPA Entities

***Needed Fields***
All `*Db` entities must have the following fields (supplied by `BaseDb`):

- `protected Long id;`
- `protected String extid;`
- `protected LocalDateTime createdAt;`
- `protected LocalDateTime updatedAt;`
- `protected LocalDateTime deletedAt;`
- `protected ActiveEnum active;`

Do not use the `deletedAt` field to check for active or inactive — use the `active` field.

### 2. Spring Data JPA Repositories

**Features:**
- Standard CRUD operations
- Custom query methods (e.g. `findByExtidIn()` on `FoodRepository` — added to collapse an N+1
  in foundation validation; see `DESIGN_DECISIONS.md` §6)
- Pagination and sorting (used by `Company` only)
- Query derivation from method names
- Native SQL queries when needed

### 3. Liquibase Database Migrations

**Features:**
- Version-controlled schema changes
- Automatic migration on startup
- Data seeding and reference data (see `../csv-load/liquibase-csv-loading-pattern.md`)

**Migration Files:**
- YAML changesets in `src/main/resources/db/changelog/changes/`
- Master changelog in `db.changelog-master.yaml`
- Tracked in the `databasechangelog` table

⚠️ **`spring.liquibase.drop-first` is OFF** in `application.yml`. Consequence: **edits to an
already-applied changeset do not take effect on startup.** Rebuild the database via the n8n
`clear-cpss-db` webhook (`GET http://localhost:5678/webhook/clear-cpss-db`) for those. New
changesets still apply normally.

This project is not in production, so schema changes are sometimes made by editing existing
changeset files rather than adding new ones — but note that the history here shows the opposite
pattern for anything structural: `012-remove-serving-from-food.yaml` and
`014-drop-serving-table.yaml` are explicit, reviewable changesets rather than edits to `002`.
That is the better habit and the reason `ddl-auto` was rejected (`DESIGN_DECISIONS.md` §12).

## Architecture

```
┌────────────────▼────────────────────────┐
│  Database Layer                         │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  Database Services (*DbService) │   │
│  │  (CRUD + soft delete)           │   │
│  └────────────┬────────────────────┘   │
│               │                         │
│  ┌────────────▼────────────────────┐   │
│  │  Mappers (Entity ↔ Domain)      │   │
│  │  (ModelMapper)                  │   │
│  └────────────┬────────────────────┘   │
│               │                         │
│  ┌────────────▼────────────────────┐   │
│  │  JPA Repositories               │   │
│  └────────────┬────────────────────┘   │
│               │                         │
│  ┌────────────▼────────────────────┐   │
│  │  JPA Entities (*Db extends BaseDb) │ │
│  └────────────┬────────────────────┘   │
└───────────────┼─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│  Spring Data JPA / Hibernate            │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│  Liquibase (schema migration)           │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│  MySQL — connection from RDS_* in .env  │
└─────────────────────────────────────────┘
```

## Dependencies

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-validation'
runtimeOnly 'com.mysql:mysql-connector-j'
implementation 'org.liquibase:liquibase-core'
compileOnly 'org.projectlombok:lombok'
```

## Current Tables

Verified 2026-08-12 from `src/main/resources/db/changelog/changes/` (changesets `001`–`020`).
All extend the standard base fields (`id`, `extid`, `created_at`, `updated_at`, `deleted_at`,
`active`). Column-level detail and the reasoning behind the shape is in `DOMAIN_MODEL.md`.

**Food core** — `food`, `nutrition`

**Salads** — `salad`, `salad_food_ingredient`

**Mixtures** — `mixture`, `mixture_ingredient`

**Auth** — `users` (login identity), `password_reset_token`

**Inherited scaffolding** — `company` (the first entity scaffolded from the layered template;
it has a full REST + pagination stack and no UI in front of it)

**Dropped** — `serving` (removed in `014-drop-serving-table.yaml`, replaced by the single
`food.typical_serving_grams` column once per-100g normalization made unit conversion
unnecessary). `flavor` was likewise folded into four integer columns on `food`
(crunch, punch, sweet, savory). See `DOMAIN_MODEL.md` and `DESIGN_DECISIONS.md` §5.

## Key Features

- JPA entities extending `BaseDb`, one per table
- Spring Data repositories
- `*DbService` classes (extend `BaseDbService`) — CRUD with soft-delete support. These throw
  `ServiceException`; business services throw `ValidationException` /
  `ResourceNotFoundException`. That distinction is invisible in the shape of the code and is
  the kind of thing the generation skills exist to get right (`DESIGN_DECISIONS.md` §11).
- Liquibase migrations, `includeAll` on the `changes/` directory
- Soft deletes via `ActiveEnum` — records are never hard-deleted, and nothing purges them
  (`DESIGN_DECISIONS.md` §3)
- ModelMapper-based entity ↔ domain mappers (note: no enum validation — see
  `../code-style/enum-data-issue.md`)
