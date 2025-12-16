package com.seibel.cpss.loader;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.Nutrition;
import com.seibel.cpss.common.domain.Mixture;
import com.seibel.cpss.common.domain.MixtureIngredient;
import com.seibel.cpss.database.db.entity.FoodDb;
import com.seibel.cpss.database.db.entity.NutritionDb;
import com.seibel.cpss.database.db.entity.MixtureDb;
import com.seibel.cpss.database.db.repository.FoodRepository;
import com.seibel.cpss.database.db.repository.NutritionRepository;
import com.seibel.cpss.database.db.repository.MixtureRepository;
import com.seibel.cpss.database.db.repository.MixtureIngredientRepository;
import com.seibel.cpss.database.db.service.FoodDbService;
import com.seibel.cpss.database.db.service.NutritionDbService;
import com.seibel.cpss.database.db.service.MixtureDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data loader that reads CSV files and loads data through DbService layer.
 * Runs on application startup and only loads if tables are empty.
 */
@Slf4j
@Component
@Order(1) // Run early in startup sequence
public class DataLoader implements CommandLineRunner {

    private final FoodDbService foodDbService;
    private final NutritionDbService nutritionDbService;
    private final MixtureDbService mixtureDbService;

    // Repositories needed for linking relationships
    private final FoodRepository foodRepository;
    private final NutritionRepository nutritionRepository;
    private final MixtureRepository mixtureRepository;
    private final MixtureIngredientRepository mixtureIngredientRepository;

    private static final String DATA_PATH = "db/data/";

    // Category list for organizing CSV files
    private static final List<String> CATEGORIES = Arrays.asList(
            "cheese",
            "dried-crunch",
            "dried-fruit",
            "fresh-fruit",
            "nuts",
            "vegetables"
    );

    public DataLoader(FoodDbService foodDbService,
                      NutritionDbService nutritionDbService,
                      MixtureDbService mixtureDbService,
                      FoodRepository foodRepository,
                      NutritionRepository nutritionRepository,
                      MixtureRepository mixtureRepository,
                      MixtureIngredientRepository mixtureIngredientRepository) {
        this.foodDbService = foodDbService;
        this.nutritionDbService = nutritionDbService;
        this.mixtureDbService = mixtureDbService;
        this.foodRepository = foodRepository;
        this.nutritionRepository = nutritionRepository;
        this.mixtureRepository = mixtureRepository;
        this.mixtureIngredientRepository = mixtureIngredientRepository;
    }

    @Override
    public void run(String... args) {
        log.info("=== Starting Data Loader ===");

        try {
            // Check if data already exists
            long foodCount = foodRepository.count();
            if (foodCount > 0) {
                log.info("Data already loaded ({} food items exist). Skipping data load.", foodCount);
                return;
            }

            log.info("No existing data found. Loading from CSV files...");

            // Load in order: Nutrition -> Food -> Link relationships -> Mixtures (with ingredients)
            loadNutrition();
            loadFoods();
            linkFoodRelationships();
            loadMixtures();

            log.info("=== Data Loading Complete ===");
            logSummary();

        } catch (Exception e) {
            log.error("FATAL: Data loading failed", e);
            throw new RuntimeException("Failed to load initial data", e);
        }
    }

    private void loadNutrition() throws IOException {
        log.info("Loading Nutrition...");
        int count = 0;

        for (String category : CATEGORIES) {
            String filePath = DATA_PATH + "40-nutrition-" + category + ".csv";
            List<Map<String, String>> records = CsvParser.parse(filePath);

            for (Map<String, String> record : records) {
                Nutrition nutrition = new Nutrition();
                nutrition.setCode(record.get("code"));
                nutrition.setName(record.get("name"));
                nutrition.setDescription(record.get("description"));
                nutrition.setNotes(record.get("notes"));
                nutrition.setCarbohydrate(parseInteger(record.get("carbohydrate")));
                nutrition.setFat(parseInteger(record.get("fat")));
                nutrition.setProtein(parseInteger(record.get("protein")));
                nutrition.setSugar(parseInteger(record.get("sugar")));
                nutrition.setVitaminD(parseInteger(record.get("vitamin_d")));
                nutrition.setVitaminE(parseInteger(record.get("vitamin_e")));

                nutritionDbService.create(nutrition);
                count++;
            }
        }

        log.info("Loaded {} nutrition profiles", count);
    }

    private void loadFoods() throws IOException {
        log.info("Loading Foods...");
        int count = 0;

        for (String category : CATEGORIES) {
            String filePath = DATA_PATH + "10-food-" + category + ".csv";
            List<Map<String, String>> records = CsvParser.parse(filePath);

            for (Map<String, String> record : records) {
                Food food = new Food();
                // Code will be auto-generated by FoodDbService
                food.setName(record.get("name"));
                food.setCategory(record.get("category"));
                food.setSubcategory(record.get("subcategory"));
                food.setDescription(record.get("description"));
                food.setNotes(record.get("notes"));
                food.setFoundation(parseBoolean(record.get("foundation")));
                food.setMixable(parseBoolean(record.get("mixable")));

                foodDbService.create(food);
                count++;
            }
        }

        log.info("Loaded {} food items", count);
    }

    /**
     * Links Food entities with their corresponding Nutrition entities
     * by matching on the 'name' field.
     */
    private void linkFoodRelationships() {
        log.info("Linking Food relationships...");

        List<FoodDb> allFoods = foodRepository.findAll();
        int linkedNutrition = 0;

        for (FoodDb food : allFoods) {
            boolean updated = false;

            // Link Nutrition by name
            Optional<NutritionDb> nutrition = nutritionRepository.findByName(food.getName());
            if (nutrition.isPresent()) {
                food.setNutrition(nutrition.get());
                linkedNutrition++;
                updated = true;
            } else {
                log.warn("No nutrition profile found for food: {}", food.getName());
            }

            if (updated) {
                foodRepository.save(food);
            }
        }

        log.info("Linked {} nutrition profiles to food items", linkedNutrition);
    }

    private void loadMixtures() throws IOException {
        log.info("Loading Mixtures with Ingredients...");

        // First, read all ingredients and group by mixture name
        String ingredientsPath = DATA_PATH + "60-mixture-ingredient.csv";
        List<Map<String, String>> ingredientRecords = CsvParser.parse(ingredientsPath);

        // Group ingredients by mixture name
        Map<String, List<Map<String, String>>> ingredientsByMixture = ingredientRecords.stream()
                .collect(Collectors.groupingBy(record -> record.get("mixture_name")));

        // Now load mixtures with their ingredients
        String mixturesPath = DATA_PATH + "50-mixture.csv";
        List<Map<String, String>> mixtureRecords = CsvParser.parse(mixturesPath);
        int count = 0;
        int ingredientCount = 0;

        for (Map<String, String> record : mixtureRecords) {
            String mixtureName = record.get("name");

            Mixture mixture = new Mixture();
            mixture.setName(mixtureName);
            mixture.setDescription(record.get("description"));
            // Leave userExtid as null for prebuilt/system mixtures

            // Add ingredients if they exist
            List<MixtureIngredient> ingredients = new ArrayList<>();
            List<Map<String, String>> mixtureIngredients = ingredientsByMixture.get(mixtureName);

            if (mixtureIngredients != null) {
                for (Map<String, String> ingredientRecord : mixtureIngredients) {
                    String foodName = ingredientRecord.get("food_name");
                    Integer grams = parseInteger(ingredientRecord.get("grams"));

                    // Find food by name to get its extid
                    Optional<FoodDb> foodOpt = foodRepository.findByName(foodName);
                    if (foodOpt.isEmpty()) {
                        log.warn("Food not found for ingredient: {}", foodName);
                        continue;
                    }

                    MixtureIngredient ingredient = new MixtureIngredient();
                    ingredient.setFoodExtid(foodOpt.get().getExtid());
                    ingredient.setGrams(grams);
                    // BaseDb fields will be set by the service

                    ingredients.add(ingredient);
                    ingredientCount++;
                }
            }

            mixture.setIngredients(ingredients);
            mixtureDbService.create(mixture);
            count++;
        }

        log.info("Loaded {} prebuilt mixtures with {} ingredients", count, ingredientCount);
    }

    private void logSummary() {
        long foods = foodRepository.count();
        long nutrition = nutritionRepository.count();
        long mixtures = mixtureRepository.count();

        log.info("=== Data Load Summary ===");
        log.info("Foods:     {}", foods);
        log.info("Nutrition: {}", nutrition);
        log.info("Mixtures:  {}", mixtures);
        log.info("========================");
    }

    // Utility parsing methods

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse integer: {}", value);
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
