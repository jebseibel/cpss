package com.seibel.cpss.service.agent;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.Nutrition;
import com.seibel.cpss.common.enums.WarningCategoryEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers triage response parsing. The API is never called — {@link AnthropicClient}
 * is constructed with no key and used only for its Jackson helpers, so these tests
 * exercise the parsing logic that stands between a model response and the database.
 */
class FoodTriageServiceTest {

    private final AnthropicClient client = new AnthropicClient("", "test-model", 1024);
    private final FoodTriageService service = new FoodTriageService(client);

    @Test
    void parsesAbstention() {
        TriageResult result = service.parse("""
                {"needs_research": false, "suspected_categories": [], "reasoning": "Plain lettuce."}
                """);

        assertFalse(result.isNeedsResearch());
        assertFalse(result.isFailed());
        assertTrue(result.getSuspectedCategories().isEmpty());
        assertEquals("Plain lettuce.", result.getReasoning());
    }

    @Test
    void parsesFlagWithCategories() {
        TriageResult result = service.parse("""
                {"needs_research": true, "suspected_categories": ["oxalate", "vitamin_k"],
                 "reasoning": "Leafy green."}
                """);

        assertTrue(result.isNeedsResearch());
        assertEquals(2, result.getSuspectedCategories().size());
        assertTrue(result.getSuspectedCategories().contains(WarningCategoryEnum.OXALATE));
        assertTrue(result.getSuspectedCategories().contains(WarningCategoryEnum.VITAMIN_K));
    }

    @Test
    void stripsCodeFences() {
        TriageResult result = service.parse("""
                ```json
                {"needs_research": false, "suspected_categories": [], "reasoning": "Fine."}
                ```
                """);

        assertFalse(result.isFailed());
        assertEquals("Fine.", result.getReasoning());
    }

    @Test
    void ignoresUnknownCategoriesButKeepsKnownOnes() {
        TriageResult result = service.parse("""
                {"needs_research": true, "suspected_categories": ["oxalate", "made_up_concern"],
                 "reasoning": "Mixed."}
                """);

        assertTrue(result.isNeedsResearch());
        assertEquals(1, result.getSuspectedCategories().size());
        assertEquals(WarningCategoryEnum.OXALATE, result.getSuspectedCategories().get(0));
    }

    /**
     * A flag with no recognizable category gives the research loop nothing to
     * work from, so it is downgraded to an abstention rather than spending tool
     * calls on an unfocused search.
     */
    @Test
    void downgradesFlagWithOnlyUnknownCategoriesToAbstention() {
        TriageResult result = service.parse("""
                {"needs_research": true, "suspected_categories": ["nonsense"],
                 "reasoning": "Unclear."}
                """);

        assertFalse(result.isNeedsResearch());
        assertFalse(result.isFailed());
    }

    @Test
    void unparseableResponseIsAFailureNotAnAbstention() {
        TriageResult result = service.parse("I think this food is probably fine, honestly.");

        assertTrue(result.isFailed());
        assertFalse(result.isNeedsResearch());
    }

    @Test
    void describeFoodIncludesNutritionAndServing() {
        Food food = Food.builder()
                .name("Spinach")
                .category("Vegetable")
                .subcategory("Leafy")
                .description("Baby spinach leaves")
                .typicalServingGrams(30)
                .nutrition(Nutrition.builder()
                        .carbohydrate(4).protein(3).fat(0).sugar(1).fiber(2)
                        .build())
                .build();

        String described = service.describeFood(food);

        assertTrue(described.contains("Spinach"));
        assertTrue(described.contains("Vegetable / Leafy"));
        assertTrue(described.contains("30g"));
        assertTrue(described.contains("sugar: 1g"));
    }

    @Test
    void describeFoodToleratesMissingNutrition() {
        Food food = Food.builder().name("Mystery").category("Other").build();

        String described = service.describeFood(food);

        assertTrue(described.contains("Mystery"));
        assertFalse(described.contains("Per 100g"));
    }
}
