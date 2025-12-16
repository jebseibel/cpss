package com.seibel.cpss.web.controller;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.Salad;
import com.seibel.cpss.service.FoodService;
import com.seibel.cpss.service.SaladService;
import com.seibel.cpss.web.request.RequestSaladBuild;
import com.seibel.cpss.web.request.RequestSaladCreate;
import com.seibel.cpss.web.request.RequestSaladUpdate;
import com.seibel.cpss.web.response.ResponseNutrition;
import com.seibel.cpss.web.response.ResponseSalad;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/salad")
@Validated
@RequiredArgsConstructor
public class SaladController {

    private final FoodService foodService;
    private final SaladService saladService;
    private final NutritionConverter nutritionConverter;
    private final SaladConverter saladConverter;

    @PostMapping
    public ResponseEntity<ResponseSalad> create(@RequestBody RequestSaladCreate request, Authentication authentication) {
        String userExtid = authentication.getName(); // Assuming username is the extid
        Salad salad = saladConverter.toDomain(request, userExtid);
        Salad created = saladService.create(salad);
        return ResponseEntity.status(HttpStatus.CREATED).body(saladConverter.toResponse(created));
    }

    @GetMapping("/{extid}")
    public ResponseSalad getByExtid(@PathVariable String extid) {
        Salad salad = saladService.findByExtid(extid);
        return saladConverter.toResponse(salad);
    }

    @GetMapping("/user/{userExtid}")
    public List<ResponseSalad> getByUserExtid(@PathVariable String userExtid) {
        List<Salad> salads = saladService.findByUserExtid(userExtid);
        return salads.stream()
                .map(saladConverter::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping
    public List<ResponseSalad> getAll() {
        List<Salad> salads = saladService.findAll();
        return salads.stream()
                .map(saladConverter::toResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/{extid}")
    public ResponseSalad update(@PathVariable String extid, @RequestBody RequestSaladUpdate request) {
        Salad salad = saladConverter.toDomain(extid, request);
        Salad updated = saladService.update(extid, salad);
        return saladConverter.toResponse(updated);
    }

    @DeleteMapping("/{extid}")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        saladService.delete(extid);
        return ResponseEntity.noContent().build();
    }
}

// package-private converter
@org.springframework.stereotype.Component
@RequiredArgsConstructor
class SaladConverter {

    private final FoodService foodService;

    Salad toDomain(RequestSaladCreate request, String userExtid) {
        Salad salad = Salad.builder()
                .name(request.getName())
                .description(request.getDescription())
                .userExtid(userExtid)
                .build();

        // Convert food ingredients
        if (request.getFoodIngredients() != null && !request.getFoodIngredients().isEmpty()) {
            List<com.seibel.cpss.common.domain.SaladFoodIngredient> ingredients = new ArrayList<>();
            for (RequestSaladCreate.SaladIngredientRequest ingredientReq : request.getFoodIngredients()) {
                com.seibel.cpss.common.domain.SaladFoodIngredient ingredient =
                    com.seibel.cpss.common.domain.SaladFoodIngredient.builder()
                        .foodExtid(ingredientReq.getFoodExtid())
                        .grams(ingredientReq.getGrams())
                        .build();
                ingredients.add(ingredient);
            }
            salad.setFoodIngredients(ingredients);
        }

        return salad;
    }

    Salad toDomain(String extid, RequestSaladUpdate request) {
        Salad salad = Salad.builder()
                .extid(extid)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        // Convert food ingredients
        if (request.getFoodIngredients() != null && !request.getFoodIngredients().isEmpty()) {
            List<com.seibel.cpss.common.domain.SaladFoodIngredient> ingredients = new ArrayList<>();
            for (RequestSaladUpdate.SaladIngredientRequest ingredientReq : request.getFoodIngredients()) {
                com.seibel.cpss.common.domain.SaladFoodIngredient ingredient =
                    com.seibel.cpss.common.domain.SaladFoodIngredient.builder()
                        .foodExtid(ingredientReq.getFoodExtid())
                        .grams(ingredientReq.getGrams())
                        .build();
                ingredients.add(ingredient);
            }
            salad.setFoodIngredients(ingredients);
        }

        return salad;
    }

    ResponseSalad toResponse(Salad salad) {
        List<ResponseSalad.SaladFoodIngredientResponse> ingredientResponses = new ArrayList<>();
        int totalGrams = 0;

        if (salad.getFoodIngredients() != null) {
            for (com.seibel.cpss.common.domain.SaladFoodIngredient ingredient : salad.getFoodIngredients()) {
                ResponseSalad.SaladFoodIngredientResponse ingredientResponse =
                    ResponseSalad.SaladFoodIngredientResponse.builder()
                        .extid(ingredient.getExtid())
                        .foodExtid(ingredient.getFoodExtid())
                        .foodName(ingredient.getFood() != null ? ingredient.getFood().getName() : null)
                        .grams(ingredient.getGrams())
                        .build();
                ingredientResponses.add(ingredientResponse);
                totalGrams += ingredient.getGrams();
            }
        }

        // Calculate total nutrition from ingredients
        ResponseNutrition totalNutrition = calculateTotalNutrition(salad.getFoodIngredients());

        // Calculate total flavor from ingredients
        FlavorTotals flavorTotals = calculateTotalFlavor(salad.getFoodIngredients());

        return ResponseSalad.builder()
                .extid(salad.getExtid())
                .name(salad.getName())
                .description(salad.getDescription())
                .userExtid(salad.getUserExtid())
                .foodIngredients(ingredientResponses)
                .totalNutrition(totalNutrition)
                .totalCrunch(flavorTotals.totalCrunch)
                .totalPunch(flavorTotals.totalPunch)
                .totalSweet(flavorTotals.totalSweet)
                .totalSavory(flavorTotals.totalSavory)
                .totalGrams(totalGrams)
                .active(salad.getActive())
                .createdAt(salad.getCreatedAt())
                .updatedAt(salad.getUpdatedAt())
                .build();
    }

    private ResponseNutrition calculateTotalNutrition(List<com.seibel.cpss.common.domain.SaladFoodIngredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return null;
        }

        int totalCarbs = 0;
        int totalFat = 0;
        int totalProtein = 0;
        int totalSugar = 0;
        int totalFiber = 0;
        int totalVitaminD = 0;
        int totalVitaminE = 0;

        for (com.seibel.cpss.common.domain.SaladFoodIngredient ingredient : ingredients) {
            if (ingredient.getFood() != null && ingredient.getFood().getNutrition() != null) {
                com.seibel.cpss.common.domain.Nutrition nutrition = ingredient.getFood().getNutrition();
                int grams = ingredient.getGrams();

                // Scale nutrition values by grams (nutrition is per 100g)
                totalCarbs += scaleNutrient(nutrition.getCarbohydrate(), grams);
                totalFat += scaleNutrient(nutrition.getFat(), grams);
                totalProtein += scaleNutrient(nutrition.getProtein(), grams);
                totalSugar += scaleNutrient(nutrition.getSugar(), grams);
                totalFiber += scaleNutrient(nutrition.getFiber(), grams);
                totalVitaminD += scaleNutrient(nutrition.getVitaminD(), grams);
                totalVitaminE += scaleNutrient(nutrition.getVitaminE(), grams);
            }
        }

        // Calculate calories from macros (4 cal/g for carbs and protein, 9 cal/g for fat)
        int totalCalories = (totalCarbs * 4) + (totalProtein * 4) + (totalFat * 9);

        return ResponseNutrition.builder()
                .calories(totalCalories)
                .carbohydrate(totalCarbs)
                .fat(totalFat)
                .protein(totalProtein)
                .sugar(totalSugar)
                .fiber(totalFiber)
                .vitaminD(totalVitaminD)
                .vitaminE(totalVitaminE)
                .build();
    }

    private FlavorTotals calculateTotalFlavor(List<com.seibel.cpss.common.domain.SaladFoodIngredient> ingredients) {
        FlavorTotals totals = new FlavorTotals();

        if (ingredients == null || ingredients.isEmpty()) {
            return totals;
        }

        for (com.seibel.cpss.common.domain.SaladFoodIngredient ingredient : ingredients) {
            if (ingredient.getFood() != null) {
                com.seibel.cpss.common.domain.Food food = ingredient.getFood();
                int grams = ingredient.getGrams();

                // Scale flavor values by grams (flavor is per 100g)
                totals.totalCrunch += scaleFlavor(food.getCrunch(), grams);
                totals.totalPunch += scaleFlavor(food.getPunch(), grams);
                totals.totalSweet += scaleFlavor(food.getSweet(), grams);
                totals.totalSavory += scaleFlavor(food.getSavory(), grams);
            }
        }

        return totals;
    }

    private static class FlavorTotals {
        int totalCrunch = 0;
        int totalPunch = 0;
        int totalSweet = 0;
        int totalSavory = 0;
    }

    private int scaleNutrient(Integer nutrientPer100g, int grams) {
        if (nutrientPer100g == null) {
            return 0;
        }
        // Scale from per-100g to actual grams
        return (nutrientPer100g * grams) / 100;
    }

    private int scaleFlavor(Integer flavorPer100g, int grams) {
        if (flavorPer100g == null) {
            return 0;
        }
        // Scale from per-100g to actual grams
        return (flavorPer100g * grams) / 100;
    }
}
