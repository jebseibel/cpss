package com.seibel.cpss.service;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.Nutrition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers the nutrition and flavor aggregation extracted out of the salad and mixture converters.
 *
 * <p>Includes the integer-truncation cases explicitly. Those assertions document a known
 * limitation rather than a desired behavior — see {@link NutritionCalculator}'s javadoc. They are
 * here so that a future move to {@code BigDecimal} produces a visible, deliberate test failure
 * instead of silently changing what the app reports.
 */
class NutritionCalculatorTest {

    private final NutritionCalculator calculator = new NutritionCalculator();

    private Food foodWith(int carbs, int fat, int protein, int sugar, int fiber, int vitD, int vitE) {
        return Food.builder()
                .nutrition(Nutrition.builder()
                        .carbohydrate(carbs)
                        .fat(fat)
                        .protein(protein)
                        .sugar(sugar)
                        .fiber(fiber)
                        .vitaminD(vitD)
                        .vitaminE(vitE)
                        .build())
                .build();
    }

    private Food flavorFood(int crunch, int punch, int sweet, int savory) {
        return Food.builder()
                .crunch(crunch)
                .punch(punch)
                .sweet(sweet)
                .savory(savory)
                .build();
    }

    @Test
    void totalNutrition_scalesFromPer100gToActualGrams() {
        Food food = foodWith(50, 10, 20, 5, 4, 2, 6);

        NutritionCalculator.NutritionTotals totals = calculator.totalNutrition(
                List.of(new NutritionCalculator.WeightedFood(food, 200)));

        // 200g is 2x the stored per-100g values
        assertEquals(100, totals.carbohydrate());
        assertEquals(20, totals.fat());
        assertEquals(40, totals.protein());
        assertEquals(10, totals.sugar());
        assertEquals(8, totals.fiber());
        assertEquals(4, totals.vitaminD());
        assertEquals(12, totals.vitaminE());
    }

    @Test
    void totalNutrition_derivesCaloriesFromMacrosUsingAtwaterFactors() {
        Food food = foodWith(50, 10, 20, 0, 0, 0, 0);

        NutritionCalculator.NutritionTotals totals = calculator.totalNutrition(
                List.of(new NutritionCalculator.WeightedFood(food, 100)));

        // (50 carbs * 4) + (20 protein * 4) + (10 fat * 9) = 200 + 80 + 90
        assertEquals(370, totals.calories());
    }

    @Test
    void totalNutrition_sumsAcrossMultipleIngredients() {
        Food a = foodWith(10, 2, 4, 1, 1, 0, 0);
        Food b = foodWith(20, 4, 8, 2, 2, 0, 0);

        NutritionCalculator.NutritionTotals totals = calculator.totalNutrition(Arrays.asList(
                new NutritionCalculator.WeightedFood(a, 100),
                new NutritionCalculator.WeightedFood(b, 100)));

        assertEquals(30, totals.carbohydrate());
        assertEquals(6, totals.fat());
        assertEquals(12, totals.protein());
    }

    @Test
    void totalNutrition_returnsNullForNoIngredients() {
        // Null rather than a zeroed total: "no ingredients" is a different statement from
        // "zero of everything", and the response omits the nutrition block entirely.
        assertNull(calculator.totalNutrition(null));
        assertNull(calculator.totalNutrition(List.of()));
    }

    @Test
    void totalNutrition_skipsIngredientsWithNoFoodOrNoNutrition() {
        Food withNutrition = foodWith(10, 0, 0, 0, 0, 0, 0);
        Food withoutNutrition = Food.builder().build();

        NutritionCalculator.NutritionTotals totals = calculator.totalNutrition(Arrays.asList(
                new NutritionCalculator.WeightedFood(withNutrition, 100),
                new NutritionCalculator.WeightedFood(withoutNutrition, 100),
                new NutritionCalculator.WeightedFood(null, 100)));

        assertNotNull(totals);
        assertEquals(10, totals.carbohydrate());
    }

    @Test
    void totalNutrition_treatsNullNutrientValuesAsZero() {
        Food sparse = Food.builder()
                .nutrition(Nutrition.builder().carbohydrate(10).build())
                .build();

        NutritionCalculator.NutritionTotals totals = calculator.totalNutrition(
                List.of(new NutritionCalculator.WeightedFood(sparse, 100)));

        assertEquals(10, totals.carbohydrate());
        assertEquals(0, totals.fat());
        assertEquals(0, totals.vitaminE());
    }

    @Test
    void totalNutrition_truncatesSmallQuantitiesToZero_knownLimitation() {
        // KNOWN LIMITATION, asserted deliberately. Integer division means a food with 5g
        // carbohydrate per 100g contributes nothing at 10g: (5 * 10) / 100 == 0, not 0.5.
        // If this test starts failing, nutrition has moved to BigDecimal - update it rather
        // than restoring the truncation.
        Food food = foodWith(5, 0, 0, 0, 0, 0, 0);

        NutritionCalculator.NutritionTotals totals = calculator.totalNutrition(
                List.of(new NutritionCalculator.WeightedFood(food, 10)));

        assertEquals(0, totals.carbohydrate());
        assertEquals(0, totals.calories());
    }

    @Test
    void totalNutrition_truncationCompoundsAcrossIngredients_knownLimitation() {
        // Truncation is applied per ingredient before summing, so ten ingredients that would
        // each contribute 0.5g sum to 0 rather than 5g. Documented, not desired.
        Food food = foodWith(5, 0, 0, 0, 0, 0, 0);

        NutritionCalculator.NutritionTotals totals = calculator.totalNutrition(Arrays.asList(
                new NutritionCalculator.WeightedFood(food, 10),
                new NutritionCalculator.WeightedFood(food, 10),
                new NutritionCalculator.WeightedFood(food, 10),
                new NutritionCalculator.WeightedFood(food, 10),
                new NutritionCalculator.WeightedFood(food, 10)));

        assertEquals(0, totals.carbohydrate());
    }

    @Test
    void totalFlavor_scalesQuadrantByGrams() {
        Food food = flavorFood(8, 4, 2, 6);

        NutritionCalculator.FlavorTotals totals = calculator.totalFlavor(
                List.of(new NutritionCalculator.WeightedFood(food, 200)));

        assertEquals(16, totals.crunch());
        assertEquals(8, totals.punch());
        assertEquals(4, totals.sweet());
        assertEquals(12, totals.savory());
    }

    @Test
    void totalFlavor_sumsAcrossIngredients() {
        NutritionCalculator.FlavorTotals totals = calculator.totalFlavor(Arrays.asList(
                new NutritionCalculator.WeightedFood(flavorFood(10, 0, 0, 0), 100),
                new NutritionCalculator.WeightedFood(flavorFood(0, 10, 0, 0), 100)));

        assertEquals(10, totals.crunch());
        assertEquals(10, totals.punch());
    }

    @Test
    void totalFlavor_returnsZeroedTotalsForNoIngredients() {
        // Unlike nutrition, flavor returns zeros rather than null - matching the behavior the
        // salad converter already had, where a salad always reports a flavor profile.
        NutritionCalculator.FlavorTotals empty = calculator.totalFlavor(null);

        assertEquals(0, empty.crunch());
        assertEquals(0, empty.punch());
        assertEquals(0, empty.sweet());
        assertEquals(0, empty.savory());
        assertEquals(0, calculator.totalFlavor(List.of()).crunch());
    }

    @Test
    void totalFlavor_treatsNullFlavorValuesAsZero() {
        Food noFlavor = Food.builder().build();

        NutritionCalculator.FlavorTotals totals = calculator.totalFlavor(
                List.of(new NutritionCalculator.WeightedFood(noFlavor, 100)));

        assertEquals(0, totals.crunch());
        assertEquals(0, totals.savory());
    }
}
