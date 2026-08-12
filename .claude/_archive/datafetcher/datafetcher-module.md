# DataFetcher — Future Work

## Status

**Not built. This is a design document, not a description of existing code.**

There is no `datafetcher` package in CPSS today. Seed data currently arrives from checked-in
CSVs (`../csv-load/liquibase-csv-loading-pattern.md`) — a human types the nutrition values.
This document describes how external nutrition ingestion *would* be built when that stops
scaling, and records the design constraints already known.

> Adapted 2026-08-12 from a working implementation of this same pattern in another project
> (a clinical-trials ingester pulling ClinicalTrials.gov and Epic FHIR). The domain is
> irrelevant here; the pattern, the measured bottleneck, and the traps are what transferred.
> Where a number below comes from that implementation rather than from CPSS, it says so.

## Why CPSS Would Want It

The food catalog is ~166 foods, hand-entered. Per-100g normalization is the tedious part of
adding a food (`DESIGN_DECISIONS.md` §5), and hand-entry is both the bottleneck and the
likeliest source of wrong data — nothing validates that a food's macros are plausible.

The obvious external source is **USDA FoodData Central**, which publishes per-100g nutrient
values under a public-domain license with a free REST API. That maps almost exactly onto the
CPSS `nutrition` table, which is the reason this is worth doing at all rather than a
speculative integration.

What it would *not* supply: the flavor quadrant (crunch, punch, sweet, savory), `foundation`,
`mixable`, or `typicalServingGrams`. Those are CPSS's own domain judgments and would stay
hand-curated. **An ingester fills in nutrition; it does not fill in the model.** That split is
the single most important design point on this page.

## The Pattern: Stage, Then Normalize

Fetch raw payloads into a staging table first; a separate step normalizes them into the real
schema.

```
POST /api/ingestion/usda            (IngestionController)
        │
FoodDataCentralClient — pages the search endpoint
        │
StagingRawFoodDbService.create(...)  — one row per food, raw JSON preserved verbatim
        │                              dedup on (source_id, source_food_id)
NutritionNormalizationService.normalizePending(maxRows)
        │
FoodRowNormalizer.normalize(staging)  — per row, own @Transactional
        │  1. Parse rawPayload → NormalizedFood
        │  2. Match to an existing Food (by name? by curated mapping table?) — see Open Questions
        │  3. Upsert the Nutrition row
        │
Returns a summary (foodsFetched, stagingRowsWritten, pendingProcessed, normalized, errors)
```

**Why stage at all**, rather than parse-and-insert in one pass: the raw payload is kept, so a
parser bug is re-runnable against the original data instead of requiring a re-fetch. It also
puts a transaction boundary per row, so one malformed record fails alone.

**Why two classes, not one.** `NutritionNormalizationService` loops and collects results;
`FoodRowNormalizer` does the per-row work inside `@Transactional`. They must be separate beans:
a self-invoked `@Transactional` method — one method on a bean calling another on the same bean —
bypasses Spring's transactional proxy entirely and is **silently non-transactional**. Splitting
forces the proxy to apply. This is a real trap and cost real debugging time in the source
implementation.

## Constraints Already Known

These are carried over from the working implementation and apply to CPSS unchanged.

**⚠️ Normalization is the bottleneck, not fetching.** Measured on 736 staging rows in the source
project: fetch ~1.1s (0.4%), staging ~0.7s (0.3%), **normalization ~257s (99.3%)** — ~349ms and
~38 DB round trips per record. Any capacity planning that budgets for network time is planning
for the wrong thing.

**A long pull will not survive an HTTP request timeout.** In the source project a full pull was
~110 minutes. An on-demand `POST` that does the whole job synchronously is the wrong shape past
a few hundred records; it needs to return a job id and report progress. CPSS already has a
progress-ticker skill (`.claude/skills/progress-ticker/`) built for exactly this kind of
long-running loop.

**`BaseDb` uses `GenerationType.IDENTITY`, which disables Hibernate JDBC batching outright.**
This is the direct cause of the round-trip count above, and it applies to CPSS because CPSS's
`BaseDb` has the same mapping. Batching child inserts is not available without changing the id
strategy — worth knowing before assuming the fix is "just batch it."

**Bound normalization independently of fetching.** A real bug in the source project: the
controller passed `maxStudies` (the fetch cap) as the normalization limit. Staging rows
accumulate across runs, so the two numbers drift and rows get silently left pending. Two
separate properties — `max-records` and `max-normalize-rows`.

**Memory on large pulls.** The source client accumulated every record in a `List<JsonNode>`
before staging any of them — ~280MB of parsed JSON in heap on a large pull. The fix is streaming
(stage each page as it arrives), not a bigger page size. Design it that way from the start.

**Dedup at the database level, not just in code.** Staging rows deduped on
`(source_id, source_record_id)` with a DB-level unique constraint, plus an existence check
before staging: a pending duplicate is skipped, an already-normalized one is refreshed in place
(new payload, `normalizedAt` cleared) rather than inserted again.

**Delete-and-reinsert children, don't diff-and-merge.** Simpler and adequate — but it means any
manual edit to a child row is lost on re-ingestion. For CPSS that matters: if someone
hand-corrects a nutrition value and a later ingest overwrites it, the correction is gone
silently. **This is the case for a "curated" flag that ingestion refuses to overwrite.**

**No rate-limit or backoff logic existed.** FoodData Central does document rate limits
(1,000 requests/hour on a demo key), so unlike CT.gov this one needs real backoff.

## Open Questions

1. **How does an ingested record match an existing CPSS food?** By name is fragile — "Almonds,
   raw" vs "almond." A curated mapping table (`food.fdc_id`) is more work and more correct.
   This is the actual hard problem and it is unsolved.
2. **What happens to `crunch`/`punch`/`sweet`/`savory` on a new food?** Ingestion cannot derive
   them. Either new foods land inactive pending curation, or ingestion only ever *updates*
   nutrition for foods that already exist. The latter is smaller and safer.
3. **Integer truncation.** CPSS nutrition columns are integers (`DESIGN_DECISIONS.md`
   outstanding item 3). USDA publishes decimals. Ingesting would round, and rounding on the way
   in is worse than rounding on the way out — this may need the `BigDecimal` migration to
   happen *first*.
4. **Where does it live?** The source project had `:datafetcher` as a separate Gradle module
   bypassing the root service layer to avoid a circular dependency. **CPSS is single-module, so
   that whole problem disappears** — an ingester here would be a package calling `*DbService`
   directly, and the architectural note that justified the bypass is not needed.

## Sketch of the Layout

```
src/main/java/com/seibel/cpss/datafetcher/
├── usda/
│   ├── FoodDataCentralClient.java        REST client, paged
│   └── UsdaIngestJob.java                fetch -> write StagingRawFood rows
├── config/
│   └── UsdaIngestProperties.java         cpss.ingestion.usda.* defaults
└── normalize/
    ├── FoodSourceParser.java             interface: raw payload -> NormalizedFood
    ├── FoodDataCentralParser.java        implements it for FDC's JSON
    ├── NormalizedFood.java               bundles Food + Nutrition
    ├── NutritionNormalizationService.java  loops pending rows, collects results
    ├── FoodRowNormalizer.java            normalizes ONE row, own transaction
    └── NormalizationCache.java           per-run lookup cache
```

Keep `FoodSourceParser` an interface from day one. The seam is where a second source plugs in,
and it costs nothing now. In the source project a scraper source was anticipated the same way
(`jsoup` sat as an unused dependency for exactly that).

## A Warning About Fixtures

From the source project, and the most transferable lesson here:

> ⚠️ **The hand-built fixture was misleading about real data.** Measured across 50 real records:
> escaped markdown survived ingestion in 23/50; bullet markers varied in ways the fixture never
> showed; 2/50 lacked expected headers entirely; the longest text field ran to 13,771 chars. A
> parser written against a hand-built fixture alone matched very little in production.

Capture real payloads early and test against those. A fixture you wrote yourself tests your
assumptions, not the source's behavior.

## Related

- `../csv-load/liquibase-csv-loading-pattern.md` — how seed data actually loads today,
  including the existing Java `DataLoader` that an ingester would sit beside
- `../database/DOMAIN_MODEL.md` — what a Food and its Nutrition are, and why they're separate
- `../../../DESIGN_DECISIONS.md` §5 (per-100g normalization), outstanding item 3 (integer
  truncation)
- `.claude/skills/progress-ticker/` — console progress for the long-running loop this needs
