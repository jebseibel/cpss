# Impact of Changes to a Db Entity File

The ripple-effect checklist: when you change a `*Db.java` entity, these are the files that must
change with it.

> **Verified against CPSS 2026-08-12.** Paths reflect CPSS's **single-module** layout — one
> Gradle module, everything under `src/main/java/com/seibel/cpss/`. This document was originally
> written against a multi-module project (`database/src/main/java/com/seibel/cancer/...`); the
> package structure was already identical, so only the paths changed. See
> `DESIGN_DECISIONS.md` §11.

> **For generating a new entity from scratch, do not use this file.** Use the skills — they are
> the maintained versions of that pattern:
> - `.claude/skills/database-restapi-template/SKILL.md` — the 9-file layered scaffold
> - `.claude/skills/database-restapi-testing/SKILL.md` — the 3 test classes + builder methods
> - `.claude/skills/entity-full-stack/SKILL.md` — both of the above, chained
> - `.claude/skills/database-column-change/SKILL.md` — the *modification* case this doc describes
>
> This doc is the human-readable version of that checklist.

## The checklist

Adding, removing, or renaming a field on `FoodDb` requires updating, in this order:

| # | File | Package |
| --- | --- | --- |
| 1 | `Food.java` (domain) | `com.seibel.cpss.common.domain` |
| 2 | `FoodMapper.java` | `com.seibel.cpss.database.db.mapper` |
| 3 | `FoodRepository.java` | `com.seibel.cpss.database.db.repository` |
| 4 | `FoodDbService.java` | `com.seibel.cpss.database.db.service` |
| 5 | `FoodService.java` | `com.seibel.cpss.service` |
| 6 | `RequestFoodCreate.java` | `com.seibel.cpss.web.request` |
| 7 | `RequestFoodUpdate.java` | `com.seibel.cpss.web.request` |
| 8 | `ResponseFood.java` | `com.seibel.cpss.web.response` |
| 9 | `FoodController.java` (+ its package-private converter, same file) | `com.seibel.cpss.web.controller` |
| 10 | `FoodMapperTest.java` | `src/test/.../db/mapper` |
| 11 | `FoodRepositoryTest.java` | `src/test/.../db/repository` |
| 12 | `FoodDbServiceTest.java` | `src/test/.../db/service` |
| 13 | `DomainBuilderDatabase.java` — append/adjust builder methods | `src/test/java/com/seibel/cpss/testutils` |
| 14 | The entity's Liquibase changeset | `src/main/resources/db/changelog/changes/` |

**For a food-related change, add a 15th step:** the CSV seed data and the loader. `FoodDb`
columns are populated from `10-food-<category>.csv` by
`src/main/java/com/seibel/cpss/loader/DataLoader.java`, not by Liquibase — a new non-nullable
column needs a value in all 15 category CSVs *and* a line in `DataLoader`'s row-mapping code.
See `../csv-load/liquibase-csv-loading-pattern.md`. This is the step most easily missed, because
the app compiles and the tests pass; only startup seeding breaks.

## Things that are easy to miss

- **The converter is inside the controller file**, not a separate class. It is package-private
  and it is where extid ↔ numeric-id resolution happens.
- **`validateUpdateRequest()`** in that converter enumerates every field to check "at least one
  provided". A new field must be added to that condition or partial updates will silently
  accept an empty request.
- **`DomainBuilderDatabase` is append-only** and shared by every entity's tests — 15 public
  static methods as of 2026-08-12, with a further 46 in `DomainBuilderUtils`. Do not reorder or
  renumber it.
- **Changeset edits do not re-apply on startup.** `spring.liquibase.drop-first` is off
  (`application.yml`), so a column added to an already-applied changeset needs a database rebuild
  (`GET http://localhost:5678/webhook/clear-cpss-db`) before it exists.
- **Quote comma-bearing column types**: `type: "decimal(10,2)"`. Unquoted, Liquibase's YAML
  parser treats the comma as a map separator and aborts the changelog.
- **The extid-only rule applies to new FK-like fields.** A new `somethingId` on the entity is
  exposed as `somethingExtid` at the REST boundary, resolved both ways in the converter. See
  `DESIGN_DECISIONS.md` §2.
- **A nutrition column is a wider change than it looks.** Nutrition values feed
  `service/NutritionCalculator`, which aggregates them for salads and mixtures. A new nutrient
  needs adding to the aggregation and to `NutritionCalculatorTest`, or it will silently read as
  zero in every total. Note also that these columns are integers — see `DESIGN_DECISIONS.md`
  outstanding item 3 before adding one that needs decimal precision.
- **A mapper omission is invisible to the compiler.** ModelMapper copies by field name, so a
  collection the mapper forgets is simply always empty. This is not hypothetical — it happened
  to `SaladMapper.toModel()` and `foodIngredients`. The `*MapperTest` exists to catch exactly
  this; assert the new field round-trips.

## Related

- `../code-style/restapi-code-style.md` — layering rules and controller style
- `database-module.md` — the persistence layer overview
- `DOMAIN_MODEL.md` — what the entities mean
- `../csv-load/liquibase-csv-loading-pattern.md` — the two seed-loading paths
