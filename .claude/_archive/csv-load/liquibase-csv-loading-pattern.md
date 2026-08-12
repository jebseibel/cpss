# CSV Data Loading Pattern

How seed data gets into the CPSS database at startup.

> **Status in this project (verified 2026-08-12). CPSS loads seed data two different ways, and
> knowing which is which will save you an hour.**
>
> 1. **Liquibase `loadData`** — `100-load-csv-data.yaml` loads `01-company.csv` and
>    `02-users.csv` only.
> 2. **A Java `CommandLineRunner`** — `src/main/java/com/seibel/cpss/loader/DataLoader.java`
>    (`@Component`, `@Order(1)`) loads the food catalog, nutrition, mixtures, and salads from
>    the remaining ~34 CSVs under `src/main/resources/db/data/`.
>
> `100-load-csv-data.yaml` says so in a comment: *"Flavor, Serving, Nutrition, and Food data
> are now loaded via Java DataLoader."* If you add a food CSV and nothing appears after
> re-running migrations, this is why — that path is not Liquibase's.
>
> **Why the split.** `loadData` inserts via raw SQL, bypassing JPA. That is fine for flat
> reference tables like `company`. It is wrong for the food catalog, which needs a `food` row
> and its matching `nutrition` row associated with each other, and for salads/mixtures, whose
> ingredient rows reference foods by `extid` — relationships that must resolve at load time.
> `DataLoader` goes through the `*DbService` layer, so that wiring happens in Java instead of
> being hand-maintained across CSV id columns.
>
> **There is no `clean_empty_strings` stored procedure in this project** and no
> `StringCleanupListener`. See *Empty strings* below.

---

# Part 1 — Liquibase `loadData` (flat reference tables)

## 1. Create the CSV

Place it in `src/main/resources/db/data/`. The convention is a numeric prefix matching load
order: `01-company.csv`, `02-users.csv`.

```csv
code,name,description
AATEST,AA Test,Test company
```

- UTF-8, comma-separated
- Header row column names must match the database column names
- Double-quote any value containing a comma
- Leave empties blank — not `""` (see *Empty strings*)

**Do not include base columns.** No `id`, `extid`, `created_at`, `updated_at`, `deleted_at`, or
`active` — those are filled by database defaults (next section).

## 2. Add a loadData changeset

This project puts Liquibase-managed CSV loads in the single `100-load-csv-data.yaml`:

```yaml
databaseChangeLog:
  - changeSet:
      id: load-csv-data
      author: jeb
      labels: load_csv_data
      changes:
        - loadData:
            tableName: company
            file: db/data/01-company.csv
            relativeToChangelogFile: false
            encoding: UTF-8
            separator: ','
            quotchar: '"'
            columns:
              - column: { name: code, type: string }
              - column: { name: name, type: string }
              - column: { name: description, type: string }
```

Key settings: `relativeToChangelogFile: false` means the path resolves from the classpath root
(`src/main/resources/`), which is why `db/data/...` works. `type: string` tells Liquibase how to
read the CSV cell — it is not the database column type.

For a single row, skip the CSV and use `insert` directly.

## 3. Master changelog picks it up automatically

`db.changelog-master.yaml` is a single `includeAll` on `db/changelog/changes`, so a new
changeset file needs no registration:

```yaml
databaseChangeLog:
  - includeAll:
      path: db/changelog/changes
```

## Base columns are handled by database defaults

`loadData` bypasses JPA entirely and inserts via raw SQL, so `BaseDb` is not involved. When a
column is absent from the INSERT, MySQL applies its `DEFAULT`. The table-creation changesets
(e.g. `001-company.yaml`) declare them:

```yaml
- column: { name: extid, type: varchar(36), constraints: { nullable: false, unique: true }, defaultValueComputed: "(UUID())" }
- column: { name: created_at, type: datetime, constraints: { nullable: false }, defaultValueComputed: CURRENT_TIMESTAMP }
- column: { name: active, type: int, defaultValueNumeric: 1 }
```

| Column | Default | Result |
| --- | --- | --- |
| `id` | AUTO_INCREMENT | sequential |
| `extid` | `(UUID())` | random UUID per row |
| `created_at` | `CURRENT_TIMESTAMP` | load time |
| `updated_at` / `deleted_at` | none | NULL |
| `active` | `1` | active |

This is a pure database feature. It works regardless of what the JPA entity says.

## Empty strings

Because `loadData` bypasses JPA, a blank CSV cell is inserted as an **empty string**, not NULL.
Nothing in this project converts them afterwards — there is no `clean_empty_strings` procedure
and no entity listener, so what the CSV contains is what lands in the table.

Options: leave the column nullable and accept `''`, pre-clean the CSV, or add a
`- sql:` step after the load. Prefer pre-cleaning the CSV — it keeps the fix visible in the data
rather than hidden in a migration.

## Changesets run once

Liquibase records executed changesets in `databasechangelog` and will not re-run one, **even if
the CSV changes**. To reload:

- Add a new changeset with a different `id`, or
- Rebuild the database (the n8n webhook — `GET http://localhost:5678/webhook/clear-cpss-db`)

Relevant here: `spring.liquibase.drop-first` is **off** in `application.yml`, which means edits
to an already-applied changeset do not take effect on startup. A rebuild is required.

---

# Part 2 — The Java `DataLoader` (the food catalog)

`src/main/java/com/seibel/cpss/loader/DataLoader.java` — a `@Component` implementing
`CommandLineRunner` with `@Order(1)`, so it runs early in startup, after Liquibase has created
the schema.

It injects both `*DbService` beans (`FoodDbService`, `NutritionDbService`, `MixtureDbService`,
`SaladDbService`) and the repositories, and loads the category-partitioned CSVs:

```
10-food-<category>.csv        ─┬─▶  food + nutrition, associated per row
40-nutrition-<category>.csv   ─┘
50-mixture.csv          ──▶  mixture
60-mixture-ingredient.csv ──▶  mixture_ingredient  (references food by extid)
70-salad.csv            ──▶  salad
80-salad-food-ingredient.csv ──▶ salad_food_ingredient (references food by extid)
```

The `10-`/`40-` pairs are split by category — `vegetables`, `cheese`, `nuts`, `herbs`, `oils`,
`vinegars`, `protein`, `fresh-fruit`, `dried-fruit`, `dried-crunch`, `grains`, `mushrooms`,
`spicy`, `aromatics`, `dressing-accents` — 15 categories, ~166 foods total. The category list
is held in `DataLoader` itself, so **adding a category means adding it to that list**, not just
dropping in a file.

**Why per-category files rather than one big CSV.** The nutrition data is edited by hand, row by
row, and a 166-row spreadsheet is materially harder to review than fifteen ~11-row ones. A diff
on `40-nutrition-nuts.csv` is readable; a diff on line 94 of a combined file is not.

**Idempotency is the thing to check before you touch it.** `DataLoader` runs on *every* startup,
unlike a Liquibase changeset that runs once. It guards against duplicate inserts by consulting
the repositories first. If you add a load step, it needs the same guard, or every restart
appends another copy of the catalog.

**This path ignores `drop-first` and `databasechangelog` entirely.** Rebuilding via the n8n
webhook clears the tables; `DataLoader` then repopulates on the next boot. Editing a food CSV
and restarting is enough to pick up the change *only if* the guard sees the table as empty —
otherwise clear it first.

---

## Troubleshooting

**File not found (Liquibase path)** — confirm the file is under `src/main/resources/db/data/`
and that `relativeToChangelogFile: false` is set. Forward slashes only.

**Column not found / constraint violation** — CSV header names must match the database column
names exactly, and the table must be created by an earlier-numbered changeset.

**Data doesn't load (Liquibase path)** — the changeset `id` is probably already in
`databasechangelog`. Check there before assuming the YAML is wrong.

**A food CSV change didn't appear** — you are on the `DataLoader` path, not the Liquibase path.
Check the idempotency guard and whether the table already had rows; clear the database and
restart.

**A new food category didn't load** — add it to the category list in `DataLoader`.

**YAML parse error mid-changelog** — if a column type contains a comma, quote it:
`type: "decimal(10,2)"`. Unquoted, Liquibase's YAML flow-mapping parser reads the comma as a map
separator and aborts the changelog.

## File organization

```
src/main/resources/db/
├── changelog/
│   ├── db.changelog-master.yaml          # includeAll on changes/
│   └── changes/
│       ├── 001-company.yaml              # CREATE TABLE ...
│       ├── ...
│       ├── 020-create-password-reset-token.yaml
│       └── 100-load-csv-data.yaml        # company + users only
└── data/
    ├── 01-company.csv                    ─┐ Liquibase loadData
    ├── 02-users.csv                      ─┘
    ├── 10-food-*.csv         (15 files)  ─┐
    ├── 40-nutrition-*.csv    (15 files)   │ Java DataLoader
    ├── 50-mixture.csv                     │
    ├── 60-mixture-ingredient.csv          │
    ├── 70-salad.csv                       │
    ├── 80-salad-food-ingredient.csv      ─┘
    └── backup/
```

## Related

- `../database/database-module.md` — the `*DbService` layer `DataLoader` writes through
- `../database/DOMAIN_MODEL.md` — why food and nutrition are separate tables
- `../../../DESIGN_DECISIONS.md` §12 — why Liquibase over `ddl-auto`, and why CSV for the catalog
