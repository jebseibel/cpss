# The wrong-enum-in-the-wrong-column trap

A failure mode that Pattern 2 (String-backed enum columns) cannot catch for you. Worth reading
before adding enums to this project — the conditions that allow it are all present today.

Originally written up as a specific bug in the ViroTrade project (two facility status enums
crossed). That bug is not reproduced here; the mechanism is, and that is the part worth keeping.

## The mechanism

1. Two enums exist whose values look plausible in the same slot — a record-lifecycle status and
   an external-system status, say.
2. Code writes the wrong one into the other's column. A builder call like `.status(x)` where `x`
   came from the other enum. It compiles: both are `String`.
3. The mapper copies it through. ModelMapper does a field-name-matched copy with **no enum
   validation** — a bad value is as valid as a good one.
4. The DB accepts it. The column is `varchar`, not an enum type or FK.
5. Nothing fails **until a read** calls `SomeEnum.valueOf(badValue)` and throws
   `IllegalArgumentException` — often in `toResponse()`, so an entire list endpoint 500s over
   one bad row.

The gap between the bad write and the visible failure is the dangerous part: the record that
breaks the page was written correctly-looking, hours or weeks earlier.

## Why this project is exposed

Verified against CPSS 2026-08-12:

- **Zero `@Enumerated`** anywhere in `src/main/java/com/seibel/cpss/database/db/entity/`. Every
  enum-ish field is a plain `String` (Pattern 2 — see `enum-to-db-mapping-patterns.md`).
- **Every mapper is ModelMapper.** None validate enum values.
- **The exposed columns today** are `category` and `subcategory` on `FoodDb`, and `role` on
  `UserDb`.

CPSS is less exposed than the project this was written for — it has fewer repeated column names,
so the classic two-vocabularies-one-slot collision is not set up yet. The live risk here is the
simpler sibling: **`FoodDb.category` is a closed 16-value set that nothing validated.**
`DataLoader` wrote `record.get("category")` straight through with no check, so a typo in a CSV
became a 17th category — appearing in the ingredient picker as its own filter group with one food
in it, rather than as an error. Same invisibility, one vocabulary instead of two.

`FoodCategoryValidator` now fails startup on an unrecognized category, so this specific hole is
closed. The mechanism it guards against is still worth understanding, because the *next*
unvalidated `String` column will not have a validator until someone writes one.

**A nastier variant lives next door.** `DataLoader.linkFoodRelationships()` matches food to
nutrition **by name**. A name mismatch between `10-food-nuts.csv` and `40-nutrition-nuts.csv`
leaves the food with null nutrition, and it then contributes **zero** to every salad and mixture
total — wrong numbers, no error, nothing in the UI to suggest the food is broken. That is worse
than the category case, which is at least visible as a stray filter. `FoodCategoryValidator`
checks this too.

The condition to watch for as the schema grows is a repeated column name. A `.status(...)` call
tells you nothing about *which* status vocabulary the value came from.

## Avoiding it

- **Set the enum, not a loose string.** `.category(FoodCategory.FRESH_FRUIT.name())` states the
  vocabulary at the call site; `.category(someOtherString)` hides it.
- **Never pass one enum's value into another's setter to "reuse" a matching constant.** If two
  enums share a constant name, that is a coincidence, not a relationship.
- **Guard reads on data you did not write.** CSV-loaded data qualifies: the food catalog is
  hand-edited and loaded by `DataLoader`/Liquibase, bypassing any Java-side validation, so a
  category typo enters the database unchallenged. The same applies to any future external
  ingestion (`../datafetcher/datafetcher-module.md`). Use a try/catch fallback rather than a bare
  `valueOf()`. See Rule 11 in `enum-lifecycle-rules.md`.
- **Suspect a bad write when a list endpoint throws `IllegalArgumentException` on one record.**
  The fix belongs at the write site; a catch at the read site only hides it.

The structural fix is Pattern 3 (`@Enumerated`), which makes the wrong assignment a compile
error. `enum-to-db-mapping-patterns.md` records that as deliberately deferred — so until then
this is a discipline problem, not something the type system will solve.

## Related

- `enum-to-db-mapping-patterns.md` — the three storage patterns and why Pattern 2 is current
- `enum-lifecycle-rules.md` — `fromString()` / `fromDisplayValue()` behaviour, load-table rules
