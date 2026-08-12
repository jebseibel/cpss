# Enum-to-Database Mapping Patterns

Covers how enums are stored, retrieved, and converted across layer boundaries.
For the lifecycle rules, see `enum-lifecycle-rules.md`.

> **Status in CPSS (verified 2026-08-12).** CPSS has exactly **two** enums, both in
> `src/main/java/com/seibel/cpss/common/enums/`:
>
> - `ActiveEnum` (ACTIVE=1 / INACTIVE=0) — the soft-delete flag on every table
> - `CompResult` (LESSER=-1 / EQUAL=0 / GREATER=1) — an internal comparison result
>
> There is **no `DisplayableEnum` or `InternalEnum` interface** in CPSS, and no
> `@Enumerated` anywhere in `database/db/entity/`. Pattern 2 below is therefore the
> universal pattern here by default rather than by decision — the columns that *should*
> be enums are plain `String`.
>
> Paths in this document use CPSS's single-module layout
> (`src/main/java/com/seibel/cpss/...`), not the multi-module layout it was written against.

---

## Pattern 1: Integer-backed enum (ActiveEnum)

Used exclusively for the `active` column on `BaseDb`. Stores `1`/`0`.
Handled by a custom JPA converter — no manual conversion needed in service code.

- Enum: `src/main/java/com/seibel/cpss/common/enums/ActiveEnum.java`
- Converter: `src/main/java/com/seibel/cpss/database/db/converter/ActiveEnumConverter.java`

`ActiveEnum` also exposes `ALLOWED_VALUES` (a comma-joined string of constant names), used in
validation messages so an error can name the legal values without rebuilding the list at each
call site.

---

## Pattern 2: String-backed domain values (current standard)

Entity fields are plain `String`. No JPA converter. Conversion, where it happens at all, is
manual at the layer boundary.

- DB column stores the value as written — e.g. `"USER"`, `"vegetables"`
- Service layer receives a raw `String` from the domain object
- Controller `toResponse()` passes it through as-is
- Controller inbound accepts whatever the client sent

**Current String columns in CPSS that behave like enums:**

| Column | Entity | Vocabulary |
| --- | --- | --- |
| `role` | `UserDb` | defaults to `"USER"`; no other value is assigned anywhere |
| `category` | `FoodDb` | 16 values (`Vegetable`, `Cheese`, `Nuts`, `Crunch`, `Seeds`, …) |
| `subcategory` | `FoodDb` | 47 values (`Tree Nut`, `Leafy Green`, `Stone Fruit`, …) |

⚠️ **`FoodDb.category` is the one that matters.** The 16 values are a closed set in practice —
they are the grouping the UI renders and the values the ingredient-picker filter offers. But
nothing enforces the set: the column is a bare `String`, `DataLoader` writes
`record.get("category")` straight through `setCategory()` with no validation, and the mappers are
ModelMapper. A typo in a CSV (`"Vegtable"`) becomes a 17th category silently — it appears in the
picker as its own filter group containing one food. Nothing fails.

This is the clearest candidate in CPSS for an actual enum, and the reason these rules are
worth keeping. See `enum-data-issue.md` for the general mechanism.

> **Two things this doc previously got wrong — corrected 2026-08-12 against the code.**
>
> **The `CATEGORIES` list in `DataLoader` is a filename list, not a category vocabulary.** It is
> interpolated into `"10-food-" + category + ".csv"` and `"40-nutrition-" + category + ".csv"`.
> The category *value* is read from the CSV's own `category` column and is never compared against
> it. That is why the two counts differ: 15 filenames, 16 categories — `10-food-dried-crunch.csv`
> legitimately contains both `Crunch` and `Seeds`. A filename is not a category.
>
> **The frontend does not hardcode the category list.** `SaladBuilder.tsx` derives it at runtime
> from the foods it receives:
> ```ts
> const cats = new Set(foods.map((f: Food) => f.category).filter(Boolean) as string[]);
> ```
> So there is no duplicated vocabulary and no backend/frontend drift risk here. The single
> authority is whatever is in the `category` column, which is exactly why validating *that* is
> the fix — and what `FoodCategoryValidator` now does at startup.

---

## Pattern 3: JPA @Enumerated (future — not implemented)

Entity field would be the enum type directly. JPA handles `name()` ↔ DB automatically.
Eliminates manual conversion entirely.

**Decision:** Deferred. Pattern 2 is the current standard. A `category` enum would need the
Liquibase column, the 15 food CSVs, and `FoodCategoryValidator`'s expected set moved together —
but *not* the frontend, which derives its filter list from the data at runtime. The startup
validator is the cheap interim measure: it catches a bad value without committing to the
migration.

---

## Conversion Points in Pattern 2

`name()` would flow through the entire system; the only boundary where a display value matters
is the frontend rendering layer.

| Direction | Where | Method |
|-----------|-------|--------|
| DB → Controller response | Controller `toResponse()` | Pass `name()` as-is |
| FE → Controller inbound | Controller inbound | `Enum.valueOf(...)` to validate, store `name()` |
| Controller → FE rendering | Frontend only | FE resolves the display value locally |

There is no `GET /api/enums/{name}` endpoint in CPSS, and no frontend enum cache. Display
strings are whatever the page hardcodes.

---

## Considered and Deferred

- **Override `toString()` to return a display value** — skipped. Keeps `toString()` as `name()`
  for consistent logging.
- **`@JsonValue` / `@JsonCreator`** — skipped. Not needed while entity fields are `String`.
- **`Optional`-returning `fromString()`** — skipped. A `getDefault()` fallback is simpler.

---

## Related Docs

- `enum-lifecycle-rules.md` — lifecycle rules
- `enum-data-issue.md` — how unvalidated String columns go wrong
- `restapi-code-style.md` — controller/service/converter layering
