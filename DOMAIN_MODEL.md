# CPSS Domain Model

How the food/flavor/nutrition domain is modeled, and why it's shaped this way.

**Verified against the code: 2026-08-12.** Every claim below was checked against the
files named in it rather than inferred from planning documents. Where the code and the
older planning docs disagree, the code wins and the disagreement is noted.

---

## The idea being modeled

CPSS comes from a cooking system, not from a database design. The premise: a salad works
when it balances four dimensions — **crunch, punch, sweet, savory** — over a **base**.
Get the balance right and the salad is worth eating without dressing.

That domain rule is the reason this isn't a generic CRUD app, and it drove three modeling
decisions that a straightforward "food tracker" schema would not have produced:

1. Flavor is a **first-class scored attribute on every food**, not a tag or a category.
2. A salad is **structurally required to have a base** — enforced in the service layer.
3. Mixtures and salads are **different kinds of things**, and only one of them has flavor.

---

## Core entities

All domain models live in `common/domain/` and extend `BaseDomain`
(`src/main/java/com/seibel/cpss/common/domain/BaseDomain.java`), which supplies:

| Field | Purpose |
| --- | --- |
| `id` (Long) | Internal database id — **never leaves the backend** |
| `extid` (String) | UUID exposed to the frontend |
| `createdAt` / `updatedAt` / `deletedAt` | Timestamps; `deletedAt` drives soft delete |
| `active` (`ActiveEnum`) | ACTIVE / INACTIVE status |

### Food

`common/domain/Food.java` — the center of the model.

```java
private String code;                  // stable business key, e.g. BELL-PEPPER-MIX
private String name;
private String category;              // Vegetables, Cheese, Nuts, Dried Fruit, ...
private String subcategory;
private Integer crunch;               // ─┐
private Integer punch;                //  │ the CPSS flavor profile
private Integer sweet;                //  │ per 100g, like nutrition
private Integer savory;               // ─┘
private Nutrition nutrition;          // per-100g nutritional facts
private Integer typicalServingGrams;
private Boolean foundation;           // can this be a salad base?
private Boolean mixable;              // can this go in a dry mixture?
```

Two things worth noticing.

**Flavor lives directly on Food, not in a related table.** Crunch/punch/sweet/savory are
four `Integer` columns alongside name and category. They are intrinsic properties of the
food in the same way carbohydrate content is — an almond is crunchy whether or not anyone
puts it in a salad. Modeling them as a separate `Flavor` entity would add a join and an
identity to something that has no independent existence.

> **Note on older docs:** `.claude/ARCHITECTURE.md` describes `Flavor` and `Serving` as
> separate entities with one-to-one relationships to Food. Neither exists in the code
> today. Flavor was folded into `Food` as the four integer columns above, and the serving
> table was dropped outright in changeset `014-drop-serving-table.yaml`, replaced by the
> single `typicalServingGrams` field. That doc predates both changes.

**`foundation` and `mixable` are capability flags, not categories.** They answer "what
role can this food play?" rather than "what kind of food is this?" — a question the
category field already answers. Romaine can be a base; sunflower seeds cannot. Keeping
them separate from `category` means a food can be recategorized without silently changing
what it's allowed to do in a recipe.

### Nutrition

`common/domain/Nutrition.java` — carbohydrate, fat, protein, sugar, fiber, vitaminD,
vitaminE. All `Integer`, all **standardized per 100g**.

Per-100g normalization is the decision that makes the whole calculation engine possible.
Every food is stored once at a common denominator, and any quantity is derived by scaling.
Storing "per typical serving" instead would mean every cross-food calculation had to
first convert between incompatible units.

### Salad and SaladFoodIngredient

A `Salad` holds a list of `SaladFoodIngredient`, each of which is a `foodExtid` plus
`grams`. That's the whole structure — a salad is a weighted bag of foods.

`Mixture` and `MixtureIngredient` mirror this shape exactly.

---

## The distinction that carries the domain: Mixture vs. Salad

These look like the same entity and are deliberately not.

| | Mixture | Salad |
| --- | --- | --- |
| What it is | Dry blend, sprinkled on other food | A complete dish |
| Flavor profile | **No** | **Yes** |
| Base required | No | **Yes — at least one foundation ingredient** |
| Used as | An ingredient/topping | A finished thing |

A mixture is dry goods in a jar — nuts, seeds, dried fruit — added at the end. Asking for
its "crunch score" isn't meaningful in the way it is for an assembled salad, because a
mixture isn't something you eat as a dish. So mixtures carry nutrition but no flavor
aggregation.

This is the kind of distinction that's cheap to collapse ("they're both lists of foods
with grams, make one entity with a type flag") and expensive to un-collapse later, because
the flavor rules and the validation rules diverge immediately.

---

## The calculation engine

Both salads and mixtures compute totals by scaling each ingredient from its per-100g
values to the actual gram weight. From `SaladController.java`:

```java
private int scaleNutrient(Integer nutrientPer100g, int grams) {
    if (nutrientPer100g == null) {
        return 0;
    }
    return (nutrientPer100g * grams) / 100;
}
```

Flavor scales identically (`scaleFlavor`), and calories are **derived, never stored**:

```java
int totalCalories = (totalCarbs * 4) + (totalProtein * 4) + (totalFat * 9);
```

4 cal/g for carbohydrate and protein, 9 for fat. Deriving calories rather than storing
them means the number can never disagree with the macros it came from — a stored calorie
field would be a second source of truth that drifts the moment a macro is corrected.

### Known limitation: integer arithmetic loses precision

**This is a real and currently-unfixed weakness, and it's worth stating plainly.**

Nutrition values are `Integer` all the way down — the domain model, and the database
(`004-nutrition.yaml` declares `carbohydrate` as `type: int`). Combined with integer
division in `scaleNutrient`, small quantities truncate toward zero:

- A food with 5g carbohydrate per 100g, used at 10g → `(5 * 10) / 100` = **0**, not 0.5.
- Any ingredient contributing under 100g of a single-digit nutrient rounds away entirely.

Micronutrients are hit hardest. The vegetable CSV
(`src/main/resources/db/data/40-nutrition-vegetables.csv`) shows most vitamin D and E
values as `0`–`2` per 100g, so at realistic salad portions they contribute nothing to the
total. The truncation also compounds — it's applied per ingredient, then summed, so a
ten-ingredient salad can lose meaningful mass.

The fix is `BigDecimal` (or scaled integers, e.g. storing tenths) through the domain,
the entity, and the Liquibase column types. It hasn't been done because it touches every
nutrition-carrying layer at once and the app's current purpose — relative flavor balance
and rough macro guidance — tolerates the error. **It would not be acceptable for anything
making a clinical or dietary claim**, which is the threshold at which this has to change.

### Resolved: the calculation was duplicated across two controllers

**Fixed 2026-08-12.** `scaleNutrient`, the calorie formula, and the aggregation loop were
previously defined identically in **both** `SaladController.java` and
`MixtureController.java`, inside their package-private converters — duplicated, so a fix to
the rounding behavior above had to be made twice and could silently be made once, and
sitting in the web layer, which by this project's own architecture does HTTP and DTO
conversion only.

They now live in `service/NutritionCalculator.java`, which both converters call. The
calculator works in `WeightedFood(food, grams)` pairs, so the two ingredient types
(`SaladFoodIngredient`, `MixtureIngredient`) reduce to a common shape without a shared
interface or a type flag on the entities.

Behavior is unchanged, including two deliberate asymmetries carried over verbatim: an empty
ingredient list yields `null` nutrition (no ingredients is a different statement from zero of
everything) but *zeroed* flavor totals (a salad always reports a quadrant). The six existing
`MixtureConverterTest` cases passed unmodified through the refactor, which is what confirms
the extracted math matches; `NutritionCalculatorTest` adds 12 more.

---

## Foundation validation — the one real business rule

Every salad must contain at least one food with `foundation = true`. Enforced in
`SaladService.validateFoundationCount()`, on both `create()` and `update()`.

```java
List<String> foodExtids = salad.getFoodIngredients().stream()
        .map(SaladFoodIngredient::getFoodExtid)
        .distinct()
        .toList();

// Fetch all foods in a single query to avoid N+1
List<Food> foods = foodService.findByExtidIn(foodExtids);

long foundationCount = foods.stream()
        .filter(food -> Boolean.TRUE.equals(food.getFoundation()))
        .count();

if (foundationCount < 1) {
    throw new ValidationException("Salad must have at least one foundation ingredient");
}
```

Three details that matter:

**It's enforced in the service, not the database.** No constraint can express "at least
one related row satisfies a predicate on a joined table." Putting it in the service means
one place to change it and a domain-specific error message instead of a constraint
violation.

**It uses a batch query.** The original implementation looked up each food individually —
a textbook N+1, where a ten-ingredient salad cost ten queries. `findByExtidIn()` was added
to `FoodRepository`, `FoodDbService`, and `FoodService` to collapse it to one. The
`.distinct()` before the query matters too: a salad listing the same food twice shouldn't
fetch it twice.

**`Boolean.TRUE.equals(...)` rather than `food.getFoundation()`.** `foundation` is a
nullable `Boolean`, so unboxing a null would throw an NPE on a food that predates the
column. This treats null as "not a foundation," which is the safe reading.

Nine tests in `SaladServiceTest.java` cover this rule, including the null and empty
ingredient-list cases.

---

## Deliberately not built

**Mixtures cannot be added to salads.** The design (`.claude/salad-plan.md`) specifies a
`salad_mixture_ingredient` table and a full scaling formula for it, and neither exists.
A salad is food ingredients only.

The unresolved question is how to scale a mixture's nutrition into a salad. A mixture's
totals are per-batch, not per-100g, so it needs `(nutritionValue × grams) / mixture.totalGrams`
rather than the `/100` used everywhere else — a second scaling path with a different
denominator. Given the integer-truncation problem above, adding a second division stage
would compound the precision loss. That's the honest reason it's still open.

**Toxicity and health warnings are not implemented.** Flagged as required in
`.claude/CLAUDE.md`: warnings for possible side effects, and diabetes-relevant guidance.
The app currently presents sugar as a bare number with no context. This is the highest
-priority outstanding domain gap, because it's the one where being incomplete could
actually mislead someone.
