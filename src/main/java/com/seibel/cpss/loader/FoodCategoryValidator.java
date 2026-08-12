package com.seibel.cpss.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Startup guard for the two silent-failure modes in the CSV-loaded food catalog.
 *
 * <p><b>Why this exists.</b> {@code FoodDb.category} is a closed set of values in practice, but it
 * is stored as a plain {@code String} with no enum, no constraint, and no validation in the mapper.
 * {@link DataLoader} writes {@code record.get("category")} straight through to
 * {@code Food.setCategory(...)}, so a typo in a CSV ({@code "Vegtable"}) is accepted, persisted,
 * and then surfaces in the ingredient picker as its own filter group containing one food. Nothing
 * fails and no log line stands out — the catalog is simply subtly wrong.
 *
 * <p>The second check is the more damaging one. {@code DataLoader.linkFoodRelationships()} matches
 * a food to its nutrition row <b>by name</b>, across two separate CSVs
 * ({@code 10-food-nuts.csv} and {@code 40-nutrition-nuts.csv}). A name that does not match leaves
 * the food with null nutrition, and a food with null nutrition contributes <b>zero</b> to every
 * salad and mixture total. The numbers are wrong, and nothing in the UI indicates it.
 *
 * <p><b>Why a validator and not an enum.</b> An enum is the real fix and is deliberately deferred
 * — it would need the Liquibase column, all 15 food CSVs, and this expected set migrated together.
 * See {@code DESIGN_DECISIONS.md} and
 * {@code .claude/_archive/code-style/enum-to-db-mapping-patterns.md}. This class is the cheap
 * interim measure: it converts a silent data error into a loud startup failure without committing
 * to that migration.
 *
 * <p><b>Note on {@code DataLoader.CATEGORIES}.</b> That list is a <i>filename</i> list, not a
 * category vocabulary — it is interpolated into {@code "10-food-<name>.csv"}. It is deliberately
 * not reused here: there are 15 filenames but 16 categories, because
 * {@code 10-food-dried-crunch.csv} legitimately contains both {@code Crunch} and {@code Seeds}.
 * Conflating the two is what made this bug hard to see in the first place.
 */
public final class FoodCategoryValidator {

    /**
     * The categories the catalog is known to contain, verified against the 15 {@code 10-food-*.csv}
     * files on 2026-08-12.
     *
     * <p>Adding a genuinely new category means adding it here — that is the intended friction. A
     * value reaching the database that is not on this list is far more likely to be a typo than a
     * deliberate addition.
     */
    static final Set<String> EXPECTED_CATEGORIES = Set.of(
            "Aromatic",
            "Cheese",
            "Crunch",
            "Dressing Accent",
            "Dried Fruit",
            "Fresh Fruit",
            "Grain",
            "Herb",
            "Mushroom",
            "Nuts",
            "Oil",
            "Protein",
            "Seeds",
            "Spicy",
            "Vegetable",
            "Vinegar"
    );

    private FoodCategoryValidator() {
    }

    /**
     * Checks every loaded food's category against {@link #EXPECTED_CATEGORIES}.
     *
     * @param foods name/category pairs for the loaded catalog
     * @return the problems found, most useful first; empty if the catalog is clean
     */
    public static List<String> validateCategories(Collection<FoodCategoryCheck> foods) {
        List<String> problems = new ArrayList<>();

        // Grouped by bad value rather than reported per food: one typo in a 21-row CSV would
        // otherwise produce 21 near-identical lines and bury the actual finding.
        Set<String> unexpected = new TreeSet<>();
        Set<String> missingCategory = new LinkedHashSet<>();

        for (FoodCategoryCheck food : foods) {
            String category = food.category();

            if (category == null || category.isBlank()) {
                missingCategory.add(food.name());
                continue;
            }

            if (!EXPECTED_CATEGORIES.contains(category)) {
                unexpected.add(category);
            }
        }

        for (String category : unexpected) {
            List<String> affected = foods.stream()
                    .filter(f -> category.equals(f.category()))
                    .map(FoodCategoryCheck::name)
                    .sorted()
                    .toList();

            problems.add(String.format(
                    "Unrecognized food category \"%s\" on %d food(s): %s. "
                            + "Expected one of %s. This is usually a typo in a 10-food-*.csv "
                            + "file; if the category is genuinely new, add it to "
                            + "FoodCategoryValidator.EXPECTED_CATEGORIES.",
                    category,
                    affected.size(),
                    String.join(", ", affected),
                    new TreeSet<>(EXPECTED_CATEGORIES)));
        }

        if (!missingCategory.isEmpty()) {
            problems.add(String.format(
                    "Food(s) loaded with a blank category: %s. Check the category column in the "
                            + "corresponding 10-food-*.csv.",
                    String.join(", ", missingCategory)));
        }

        return problems;
    }

    /**
     * Reports foods that failed to link to a nutrition row.
     *
     * <p>Kept separate from the category check because the cause is different — a name mismatch
     * across two CSVs rather than a bad value in one — but it belongs to the same class of silent
     * catalog corruption, and both are worth catching in the same startup pass.
     *
     * @param unlinkedFoodNames names of foods left with no nutrition after linking
     * @return the problems found; empty if every food linked
     */
    public static List<String> validateNutritionLinks(Collection<String> unlinkedFoodNames) {
        if (unlinkedFoodNames.isEmpty()) {
            return List.of();
        }

        List<String> sorted = new TreeSet<>(unlinkedFoodNames).stream().toList();

        return List.of(String.format(
                "%d food(s) have no nutrition profile and will contribute ZERO to every salad "
                        + "and mixture total: %s. Nutrition is matched by name, so this is a name "
                        + "mismatch between 10-food-*.csv and 40-nutrition-*.csv.",
                sorted.size(),
                String.join(", ", sorted)));
    }

    /**
     * A loaded food reduced to the two fields this validator cares about.
     */
    public record FoodCategoryCheck(String name, String category) {
    }
}
