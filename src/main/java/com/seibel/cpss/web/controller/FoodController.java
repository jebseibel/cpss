package com.seibel.cpss.web.controller;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.service.FoodService;
import com.seibel.cpss.service.NutritionService;
import com.seibel.cpss.web.request.RequestFoodCreate;
import com.seibel.cpss.web.request.RequestFoodUpdate;
import com.seibel.cpss.web.response.ResponseFood;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food")
@Validated
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;
    private final NutritionService nutritionService;
    private final FoodConverter converter;

    @PostMapping
    public ResponseFood create(@RequestBody RequestFoodCreate request) {
        Food created = foodService.create(converter.toDomain(request));
        return converter.toResponse(created);
    }

    @PutMapping("/{extid}")
    public ResponseFood update(@PathVariable String extid, @RequestBody RequestFoodUpdate request) {
        converter.validateUpdateRequest(request);
        Food updated = foodService.update(extid, converter.toDomain(extid, request));
        return converter.toResponse(updated);
    }

    @GetMapping
    public List<ResponseFood> getAll() {
        return converter.toResponse(foodService.findAll());
    }

    @GetMapping("/{extid}")
    public ResponseFood getByExtid(@PathVariable String extid) {
        return converter.toResponse(foodService.findByExtid(extid));
    }

    @DeleteMapping("/{extid}")
    public ResponseEntity<Void> delete(@PathVariable String extid) {
        foodService.delete(extid);
        return ResponseEntity.noContent().build();
    }
}

// package-private converter
@org.springframework.stereotype.Component
@RequiredArgsConstructor
class FoodConverter {

    private final NutritionService nutritionService;
    private final NutritionConverter nutritionConverter;

    Food toDomain(RequestFoodCreate request) {
        return Food.builder()
                .code(request.getCode())
                .name(request.getName())
                .category(request.getCategory())
                .subcategory(request.getSubcategory())
                .description(request.getDescription())
                .notes(request.getNotes())
                .crunch(request.getCrunch())
                .punch(request.getPunch())
                .sweet(request.getSweet())
                .savory(request.getSavory())
                .nutrition(request.getNutritionExtid() != null ? nutritionService.findByExtid(request.getNutritionExtid()) : null)
                .typicalServingGrams(request.getTypicalServingGrams())
                .foundation(request.getFoundation())
                .mixable(request.getMixable())
                .build();
    }

    Food toDomain(String extid, RequestFoodUpdate request) {
        return Food.builder()
                .extid(extid)
                .code(request.getCode())
                .name(request.getName())
                .category(request.getCategory())
                .subcategory(request.getSubcategory())
                .description(request.getDescription())
                .notes(request.getNotes())
                .crunch(request.getCrunch())
                .punch(request.getPunch())
                .sweet(request.getSweet())
                .savory(request.getSavory())
                .nutrition(request.getNutritionExtid() != null ? nutritionService.findByExtid(request.getNutritionExtid()) : null)
                .typicalServingGrams(request.getTypicalServingGrams())
                .foundation(request.getFoundation())
                .mixable(request.getMixable())
                .build();
    }

    ResponseFood toResponse(Food item) {
        return ResponseFood.builder()
                .extid(item.getExtid())
                .code(item.getCode())
                .name(item.getName())
                .category(item.getCategory())
                .subcategory(item.getSubcategory())
                .description(item.getDescription())
                .notes(item.getNotes())
                .crunch(item.getCrunch())
                .punch(item.getPunch())
                .sweet(item.getSweet())
                .savory(item.getSavory())
                .nutrition(item.getNutrition() != null ? nutritionConverter.toResponse(item.getNutrition()) : null)
                .typicalServingGrams(item.getTypicalServingGrams())
                .foundation(item.getFoundation())
                .mixable(item.getMixable())
                .build();
    }

    List<ResponseFood> toResponse(List<Food> items) {
        return items.stream().map(this::toResponse).toList();
    }

    void validateUpdateRequest(RequestFoodUpdate request) {
        if (request.getCode() == null &&
                request.getName() == null &&
                request.getCategory() == null &&
                request.getSubcategory() == null &&
                request.getDescription() == null &&
                request.getNotes() == null &&
                request.getCrunch() == null &&
                request.getPunch() == null &&
                request.getSweet() == null &&
                request.getSavory() == null &&
                request.getNutritionExtid() == null &&
                request.getTypicalServingGrams() == null &&
                request.getFoundation() == null &&
                request.getMixable() == null) {
            throw new IllegalArgumentException("At least one field must be provided for update.");
        }
    }
}
