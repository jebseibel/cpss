package com.seibel.cpss.loader;

import com.seibel.cpss.loader.FoodCategoryValidator.FoodCategoryCheck;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the startup guard for silent food-catalog corruption.
 *
 * <p>The failure modes asserted here are the ones that produce no error on their own: a
 * mistyped category becomes a stray filter group in the UI, and an unlinked nutrition row makes a
 * food contribute zero to every total. Both were possible before this validator existed.
 */
class FoodCategoryValidatorTest {

    private static FoodCategoryCheck food(String name, String category) {
        return new FoodCategoryCheck(name, category);
    }

    @Test
    void cleanCatalogProducesNoProblems() {
        List<FoodCategoryCheck> foods = List.of(
                food("Almonds", "Nuts"),
                food("Romaine", "Vegetable"),
                food("Feta", "Cheese"),
                food("Pumpkin Seeds", "Seeds"));

        assertTrue(FoodCategoryValidator.validateCategories(foods).isEmpty());
    }

    @Test
    void everyExpectedCategoryIsAccepted() {
        List<FoodCategoryCheck> foods = FoodCategoryValidator.EXPECTED_CATEGORIES.stream()
                .map(c -> food("food-in-" + c, c))
                .toList();

        assertTrue(FoodCategoryValidator.validateCategories(foods).isEmpty(),
                "the 16 categories present in the CSVs must all validate");
    }

    @Test
    void typoInCategoryIsReported() {
        List<FoodCategoryCheck> foods = List.of(
                food("Romaine", "Vegetable"),
                food("Kale", "Vegtable"));

        List<String> problems = FoodCategoryValidator.validateCategories(foods);

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("Vegtable"), problems.get(0));
        assertTrue(problems.get(0).contains("Kale"), problems.get(0));
    }

    /**
     * The point of grouping by bad value: one typo repeated across a 21-row CSV should read as a
     * single finding naming the affected foods, not 21 near-identical lines.
     */
    @Test
    void oneBadCategoryAcrossManyFoodsIsOneProblem() {
        List<FoodCategoryCheck> foods = List.of(
                food("Kale", "Vegtable"),
                food("Chard", "Vegtable"),
                food("Spinach", "Vegtable"));

        List<String> problems = FoodCategoryValidator.validateCategories(foods);

        assertEquals(1, problems.size());
        String problem = problems.get(0);
        assertTrue(problem.contains("3 food(s)"), problem);
        assertTrue(problem.contains("Chard"), problem);
        assertTrue(problem.contains("Kale"), problem);
        assertTrue(problem.contains("Spinach"), problem);
    }

    @Test
    void distinctBadCategoriesAreReportedSeparately() {
        List<FoodCategoryCheck> foods = List.of(
                food("Kale", "Vegtable"),
                food("Almonds", "Nutz"));

        assertEquals(2, FoodCategoryValidator.validateCategories(foods).size());
    }

    @Test
    void categoryMatchingIsCaseSensitive() {
        // "vegetable" is the CSV *filename* convention; the column value is "Vegetable". Accepting
        // both would let the two drift apart, which is the confusion this validator exists to end.
        List<String> problems = FoodCategoryValidator.validateCategories(
                List.of(food("Romaine", "vegetable")));

        assertEquals(1, problems.size());
    }

    @Test
    void blankAndNullCategoriesAreReported() {
        List<String> problems = FoodCategoryValidator.validateCategories(List.of(
                food("Mystery Food", ""),
                food("Other Food", "   ")));

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("Mystery Food"), problems.get(0));
        assertTrue(problems.get(0).contains("Other Food"), problems.get(0));
    }

    @Test
    void nullCategoryIsReportedAsBlankNotUnrecognized() {
        List<String> problems = FoodCategoryValidator.validateCategories(
                List.of(food("Nameless", null)));

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("blank category"), problems.get(0));
    }

    @Test
    void linkedNutritionProducesNoProblems() {
        assertTrue(FoodCategoryValidator.validateNutritionLinks(List.of()).isEmpty());
    }

    @Test
    void unlinkedNutritionIsReportedWithItsConsequence() {
        List<String> problems = FoodCategoryValidator.validateNutritionLinks(
                List.of("Walnuts", "Cashews"));

        assertEquals(1, problems.size());
        String problem = problems.get(0);
        assertTrue(problem.contains("Walnuts"), problem);
        assertTrue(problem.contains("Cashews"), problem);
        // The message must say why it matters, not just that it happened.
        assertTrue(problem.contains("ZERO"), problem);
        assertFalse(problem.contains("null"), problem);
    }
}
