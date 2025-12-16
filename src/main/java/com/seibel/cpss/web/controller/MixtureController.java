package com.seibel.cpss.web.controller;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.Mixture;
import com.seibel.cpss.service.FoodService;
import com.seibel.cpss.service.MixtureService;
import com.seibel.cpss.web.request.RequestMixtureCreate;
import com.seibel.cpss.web.request.RequestMixtureUpdate;
import com.seibel.cpss.web.response.ResponseNutrition;
import com.seibel.cpss.web.response.ResponseMixture;
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
@RequestMapping("/api/mixture")
@Validated
@RequiredArgsConstructor
public class MixtureController {

    private final FoodService foodService;
    private final MixtureService mixtureService;
    private final NutritionConverter nutritionConverter;
    private final MixtureConverter mixtureConverter;

    @PostMapping
    public ResponseEntity<ResponseMixture> create(@RequestBody RequestMixtureCreate request, Authentication authentication) {
        String userExtid = authentication.getName(); // Assuming username is the extid
        Mixture mixture = mixtureConverter.toDomain(request, userExtid);
        Mixture created = mixtureService.create(mixture);
        return ResponseEntity.status(HttpStatus.CREATED).body(mixtureConverter.toResponse(created));
    }

    @GetMapping("/{extid}")
    public ResponseMixture getByExtid(@PathVariable String extid) {
        Mixture mixture = mixtureService.findByExtid(extid);
        return mixtureConverter.toResponse(mixture);
    }

    @GetMapping("/user/{userExtid}")
    public List<ResponseMixture> getByUserExtid(@PathVariable String userExtid) {
        List<Mixture> mixtures = mixtureService.findByUserExtid(userExtid);
        return mixtures.stream()
                .map(mixtureConverter::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping
    public List<ResponseMixture> getAll() {
        List<Mixture> mixtures = mixtureService.findAll();
        return mixtures.stream()
                .map(mixtureConverter::toResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/{extid}")
    public ResponseMixture update(@PathVariable String extid, @RequestBody RequestMixtureUpdate request) {
        Mixture mixture = mixtureConverter.toDomain(extid, request);
        Mixture updated = mixtureService.update(extid, mixture);
        return mixtureConverter.toResponse(updated);
    }

    @DeleteMapping("/{extid}")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        mixtureService.delete(extid);
        return ResponseEntity.noContent().build();
    }
}

// package-private converter
@org.springframework.stereotype.Component
@RequiredArgsConstructor
class MixtureConverter {

    private final FoodService foodService;

    Mixture toDomain(RequestMixtureCreate request, String userExtid) {
        Mixture mixture = Mixture.builder()
                .name(request.getName())
                .description(request.getDescription())
                .userExtid(userExtid)
                .build();

        // Convert ingredients
        if (request.getIngredients() != null && !request.getIngredients().isEmpty()) {
            List<com.seibel.cpss.common.domain.MixtureIngredient> ingredients = new ArrayList<>();
            for (RequestMixtureCreate.MixtureIngredientRequest ingredientReq : request.getIngredients()) {
                com.seibel.cpss.common.domain.MixtureIngredient ingredient =
                    com.seibel.cpss.common.domain.MixtureIngredient.builder()
                        .foodExtid(ingredientReq.getFoodExtid())
                        .grams(ingredientReq.getGrams())
                        .build();
                ingredients.add(ingredient);
            }
            mixture.setIngredients(ingredients);
        }

        return mixture;
    }

    Mixture toDomain(String extid, RequestMixtureUpdate request) {
        Mixture mixture = Mixture.builder()
                .extid(extid)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        // Convert ingredients
        if (request.getIngredients() != null && !request.getIngredients().isEmpty()) {
            List<com.seibel.cpss.common.domain.MixtureIngredient> ingredients = new ArrayList<>();
            for (RequestMixtureUpdate.MixtureIngredientRequest ingredientReq : request.getIngredients()) {
                com.seibel.cpss.common.domain.MixtureIngredient ingredient =
                    com.seibel.cpss.common.domain.MixtureIngredient.builder()
                        .foodExtid(ingredientReq.getFoodExtid())
                        .grams(ingredientReq.getGrams())
                        .build();
                ingredients.add(ingredient);
            }
            mixture.setIngredients(ingredients);
        }

        return mixture;
    }

    ResponseMixture toResponse(Mixture mixture) {
        List<ResponseMixture.MixtureIngredientResponse> ingredientResponses = new ArrayList<>();
        int totalGrams = 0;

        if (mixture.getIngredients() != null) {
            for (com.seibel.cpss.common.domain.MixtureIngredient ingredient : mixture.getIngredients()) {
                ResponseMixture.MixtureIngredientResponse ingredientResponse =
                    ResponseMixture.MixtureIngredientResponse.builder()
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
        ResponseNutrition totalNutrition = calculateTotalNutrition(mixture.getIngredients());

        return ResponseMixture.builder()
                .extid(mixture.getExtid())
                .name(mixture.getName())
                .description(mixture.getDescription())
                .userExtid(mixture.getUserExtid())
                .ingredients(ingredientResponses)
                .totalNutrition(totalNutrition)
                .totalGrams(totalGrams)
                .active(mixture.getActive())
                .createdAt(mixture.getCreatedAt())
                .updatedAt(mixture.getUpdatedAt())
                .build();
    }

    private ResponseNutrition calculateTotalNutrition(List<com.seibel.cpss.common.domain.MixtureIngredient> ingredients) {
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

        for (com.seibel.cpss.common.domain.MixtureIngredient ingredient : ingredients) {
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

    private int scaleNutrient(Integer nutrientPer100g, int grams) {
        if (nutrientPer100g == null) {
            return 0;
        }
        // Scale from per-100g to actual grams
        return (nutrientPer100g * grams) / 100;
    }
}
