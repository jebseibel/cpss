package com.seibel.cpss.service;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.Nutrition;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Aggregates nutrition and flavor across a weighted list of foods.
 *
 * <p><b>Why this is a service and not converter code.</b> Scaling an ingredient from its stored
 * per-100g values to an actual gram weight, and deriving calories from macros, are business
 * rules — they decide what the numbers mean. They previously lived as private methods duplicated
 * verbatim inside the package-private converters of both {@code SaladController} and
 * {@code MixtureController}, which put domain logic in the web layer and meant any correction
 * had to be made twice and could silently be made once.
 *
 * <p><b>Per-100g is the contract.</b> Every food stores nutrition and flavor per 100g, so any
 * quantity is derived by scaling. That normalization is what lets foods with incommensurable
 * serving sizes be summed at all.
 *
 * <p><b>Calories are derived, never stored.</b> {@code (carbs x 4) + (protein x 4) + (fat x 9)},
 * the Atwater factors. A stored calorie column would be a second source of truth that drifts the
 * moment a macro is corrected.
 *
 * <p><b>Known limitation: integer truncation.</b> Nutrition is {@code Integer} end to end — the
 * domain model and the {@code int} columns in changeset {@code 004-nutrition.yaml} — and
 * {@link #scale} divides as integers. A food with 5g carbohydrate per 100g used at 10g yields 0,
 * not 0.5, and the loss compounds because truncation happens per ingredient before summing.
 * Micronutrients suffer most: most vitamin D and E values in the seed data are 0-2 per 100g, so
 * at realistic portions they contribute nothing. Fixing it means {@code BigDecimal} through the
 * domain, the entity, and the schema. Acceptable while the app is about relative flavor balance
 * and rough macro guidance; not acceptable for anything making a dietary or clinical claim.
 */
@Service
public class NutritionCalculator {

    /** One ingredient reduced to what the calculation needs: a food and how much of it. */
    public record WeightedFood(Food food, int grams) {
    }

    /** Aggregated flavor across a set of weighted foods, in the CPSS quadrant. */
    public record FlavorTotals(int crunch, int punch, int sweet, int savory) {
        public static FlavorTotals empty() {
            return new FlavorTotals(0, 0, 0, 0);
        }
    }

    /** Aggregated nutrition across a set of weighted foods. Calories are derived from macros. */
    public record NutritionTotals(int calories, int carbohydrate, int fat, int protein,
                                  int sugar, int fiber, int vitaminD, int vitaminE) {
    }

    /**
     * Sums nutrition across the given foods, scaling each by its gram weight.
     *
     * <p>Returns {@code null} for a null or empty list rather than a zeroed total, preserving the
     * behavior both converters already had: no ingredients means no nutrition to report, which is
     * a different statement from "zero of everything".
     *
     * <p>An ingredient whose food or nutrition is absent contributes nothing instead of failing,
     * so a partially-loaded graph still produces a usable total.
     */
    public NutritionTotals totalNutrition(List<WeightedFood> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return null;
        }

        int carbs = 0;
        int fat = 0;
        int protein = 0;
        int sugar = 0;
        int fiber = 0;
        int vitaminD = 0;
        int vitaminE = 0;

        for (WeightedFood ingredient : ingredients) {
            if (ingredient == null || ingredient.food() == null
                    || ingredient.food().getNutrition() == null) {
                continue;
            }
            Nutrition nutrition = ingredient.food().getNutrition();
            int grams = ingredient.grams();

            carbs += scale(nutrition.getCarbohydrate(), grams);
            fat += scale(nutrition.getFat(), grams);
            protein += scale(nutrition.getProtein(), grams);
            sugar += scale(nutrition.getSugar(), grams);
            fiber += scale(nutrition.getFiber(), grams);
            vitaminD += scale(nutrition.getVitaminD(), grams);
            vitaminE += scale(nutrition.getVitaminE(), grams);
        }

        int calories = (carbs * 4) + (protein * 4) + (fat * 9);

        return new NutritionTotals(calories, carbs, fat, protein, sugar, fiber, vitaminD, vitaminE);
    }

    /**
     * Sums the CPSS flavor quadrant across the given foods, scaling each by its gram weight.
     *
     * <p>Unlike nutrition this returns a zeroed total rather than {@code null} for an empty list,
     * again matching the existing behavior — the salad converter always produced flavor numbers.
     *
     * <p>Only salads use this. Mixtures are dry toppings and carry no flavor profile, which is
     * the distinction between the two entities.
     */
    public FlavorTotals totalFlavor(List<WeightedFood> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return FlavorTotals.empty();
        }

        int crunch = 0;
        int punch = 0;
        int sweet = 0;
        int savory = 0;

        for (WeightedFood ingredient : ingredients) {
            if (ingredient == null || ingredient.food() == null) {
                continue;
            }
            Food food = ingredient.food();
            int grams = ingredient.grams();

            crunch += scale(food.getCrunch(), grams);
            punch += scale(food.getPunch(), grams);
            sweet += scale(food.getSweet(), grams);
            savory += scale(food.getSavory(), grams);
        }

        return new FlavorTotals(crunch, punch, sweet, savory);
    }

    /**
     * Scales a per-100g value to an actual gram weight.
     *
     * <p>A null value contributes 0 rather than throwing, so a food missing a micronutrient is
     * treated as "none recorded" instead of breaking the whole total.
     *
     * <p>See the class javadoc on integer truncation — this division is where it happens.
     */
    private int scale(Integer valuePer100g, int grams) {
        if (valuePer100g == null) {
            return 0;
        }
        return (valuePer100g * grams) / 100;
    }
}
