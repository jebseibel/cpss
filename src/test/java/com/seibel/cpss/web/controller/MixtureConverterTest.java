package com.seibel.cpss.web.controller;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.Mixture;
import com.seibel.cpss.common.domain.MixtureIngredient;
import com.seibel.cpss.common.domain.Nutrition;
import com.seibel.cpss.service.FoodService;
import com.seibel.cpss.web.response.ResponseMixture;
import com.seibel.cpss.web.response.ResponseNutrition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MixtureConverterTest {

    @Mock
    private FoodService foodService;

    private MixtureConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MixtureConverter(foodService);
    }

    @Test
    void toResponse_shouldCalculateTotalNutritionCorrectly() {
        // Arrange - Create nutrition for food 1 (per 100g)
        Nutrition nutrition1 = Nutrition.builder()
                .carbohydrate(50)
                .fat(10)
                .protein(20)
                .sugar(5)
                .fiber(8)
                .vitaminD(2)
                .vitaminE(3)
                .build();

        Food food1 = Food.builder()
                .extid("food-1")
                .name("Food 1")
                .nutrition(nutrition1)
                .build();

        // Create nutrition for food 2 (per 100g)
        Nutrition nutrition2 = Nutrition.builder()
                .carbohydrate(30)
                .fat(20)
                .protein(10)
                .sugar(10)
                .fiber(5)
                .vitaminD(1)
                .vitaminE(2)
                .build();

        Food food2 = Food.builder()
                .extid("food-2")
                .name("Food 2")
                .nutrition(nutrition2)
                .build();

        // Create ingredients - 100g of each food
        MixtureIngredient ingredient1 = MixtureIngredient.builder()
                .foodExtid("food-1")
                .food(food1)
                .grams(100)
                .build();

        MixtureIngredient ingredient2 = MixtureIngredient.builder()
                .foodExtid("food-2")
                .food(food2)
                .grams(100)
                .build();

        List<MixtureIngredient> ingredients = Arrays.asList(ingredient1, ingredient2);

        Mixture mixture = Mixture.builder()
                .extid("mixture-1")
                .name("Test Mixture")
                .description("Test Description")
                .userExtid("user-1")
                .ingredients(ingredients)
                .build();

        // Act
        ResponseMixture response = converter.toResponse(mixture);

        // Assert
        assertNotNull(response);
        assertEquals("mixture-1", response.getExtid());
        assertEquals("Test Mixture", response.getName());
        assertEquals(200, response.getTotalGrams());

        // Verify nutrition calculation
        ResponseNutrition totalNutrition = response.getTotalNutrition();
        assertNotNull(totalNutrition);

        // Carbs: (50 + 30) = 80g
        assertEquals(80, totalNutrition.getCarbohydrate());

        // Fat: (10 + 20) = 30g
        assertEquals(30, totalNutrition.getFat());

        // Protein: (20 + 10) = 30g
        assertEquals(30, totalNutrition.getProtein());

        // Sugar: (5 + 10) = 15g
        assertEquals(15, totalNutrition.getSugar());

        // Fiber: (8 + 5) = 13g
        assertEquals(13, totalNutrition.getFiber());

        // Vitamin D: (2 + 1) = 3
        assertEquals(3, totalNutrition.getVitaminD());

        // Vitamin E: (3 + 2) = 5
        assertEquals(5, totalNutrition.getVitaminE());

        // Calories: (80 * 4) + (30 * 4) + (30 * 9) = 320 + 120 + 270 = 710
        assertEquals(710, totalNutrition.getCalories());
    }

    @Test
    void toResponse_shouldScaleNutritionByGrams() {
        // Arrange - Create nutrition for food (per 100g)
        Nutrition nutrition = Nutrition.builder()
                .carbohydrate(40)
                .fat(10)
                .protein(20)
                .sugar(5)
                .fiber(8)
                .vitaminD(2)
                .vitaminE(4)
                .build();

        Food food = Food.builder()
                .extid("food-1")
                .name("Food 1")
                .nutrition(nutrition)
                .build();

        // Create ingredient with 50g (half of 100g)
        MixtureIngredient ingredient = MixtureIngredient.builder()
                .foodExtid("food-1")
                .food(food)
                .grams(50)
                .build();

        Mixture mixture = Mixture.builder()
                .extid("mixture-1")
                .name("Test Mixture")
                .ingredients(Arrays.asList(ingredient))
                .build();

        // Act
        ResponseMixture response = converter.toResponse(mixture);

        // Assert
        ResponseNutrition totalNutrition = response.getTotalNutrition();
        assertNotNull(totalNutrition);

        // All values should be halved since we're using 50g instead of 100g
        assertEquals(20, totalNutrition.getCarbohydrate());
        assertEquals(5, totalNutrition.getFat());
        assertEquals(10, totalNutrition.getProtein());
        assertEquals(2, totalNutrition.getSugar());
        assertEquals(4, totalNutrition.getFiber());
        assertEquals(1, totalNutrition.getVitaminD());
        assertEquals(2, totalNutrition.getVitaminE());

        // Calories: (20 * 4) + (10 * 4) + (5 * 9) = 80 + 40 + 45 = 165
        assertEquals(165, totalNutrition.getCalories());
    }

    @Test
    void toResponse_shouldHandleNullNutrition() {
        // Arrange
        Food food = Food.builder()
                .extid("food-1")
                .name("Food 1")
                .nutrition(null)
                .build();

        MixtureIngredient ingredient = MixtureIngredient.builder()
                .foodExtid("food-1")
                .food(food)
                .grams(100)
                .build();

        Mixture mixture = Mixture.builder()
                .extid("mixture-1")
                .name("Test Mixture")
                .ingredients(Arrays.asList(ingredient))
                .build();

        // Act
        ResponseMixture response = converter.toResponse(mixture);

        // Assert
        ResponseNutrition totalNutrition = response.getTotalNutrition();
        assertNotNull(totalNutrition);

        // All values should be 0
        assertEquals(0, totalNutrition.getCarbohydrate());
        assertEquals(0, totalNutrition.getFat());
        assertEquals(0, totalNutrition.getProtein());
        assertEquals(0, totalNutrition.getSugar());
        assertEquals(0, totalNutrition.getFiber());
        assertEquals(0, totalNutrition.getVitaminD());
        assertEquals(0, totalNutrition.getVitaminE());
        assertEquals(0, totalNutrition.getCalories());
    }

    @Test
    void toResponse_shouldHandleEmptyIngredients() {
        // Arrange
        Mixture mixture = Mixture.builder()
                .extid("mixture-1")
                .name("Test Mixture")
                .ingredients(Arrays.asList())
                .build();

        // Act
        ResponseMixture response = converter.toResponse(mixture);

        // Assert
        assertNotNull(response);
        assertNull(response.getTotalNutrition());
        assertEquals(0, response.getTotalGrams());
    }

    @Test
    void toResponse_shouldHandleNullIngredients() {
        // Arrange
        Mixture mixture = Mixture.builder()
                .extid("mixture-1")
                .name("Test Mixture")
                .ingredients(null)
                .build();

        // Act
        ResponseMixture response = converter.toResponse(mixture);

        // Assert
        assertNotNull(response);
        assertNull(response.getTotalNutrition());
        assertEquals(0, response.getTotalGrams());
    }

    @Test
    void toResponse_shouldHandleNullNutrientValues() {
        // Arrange - Create nutrition with some null values
        Nutrition nutrition = Nutrition.builder()
                .carbohydrate(null)
                .fat(10)
                .protein(null)
                .sugar(5)
                .fiber(null)
                .vitaminD(null)
                .vitaminE(2)
                .build();

        Food food = Food.builder()
                .extid("food-1")
                .name("Food 1")
                .nutrition(nutrition)
                .build();

        MixtureIngredient ingredient = MixtureIngredient.builder()
                .foodExtid("food-1")
                .food(food)
                .grams(100)
                .build();

        Mixture mixture = Mixture.builder()
                .extid("mixture-1")
                .name("Test Mixture")
                .ingredients(Arrays.asList(ingredient))
                .build();

        // Act
        ResponseMixture response = converter.toResponse(mixture);

        // Assert
        ResponseNutrition totalNutrition = response.getTotalNutrition();
        assertNotNull(totalNutrition);

        // Null values should be treated as 0
        assertEquals(0, totalNutrition.getCarbohydrate());
        assertEquals(10, totalNutrition.getFat());
        assertEquals(0, totalNutrition.getProtein());
        assertEquals(5, totalNutrition.getSugar());
        assertEquals(0, totalNutrition.getFiber());
        assertEquals(0, totalNutrition.getVitaminD());
        assertEquals(2, totalNutrition.getVitaminE());

        // Calories: (0 * 4) + (0 * 4) + (10 * 9) = 90
        assertEquals(90, totalNutrition.getCalories());
    }
}
