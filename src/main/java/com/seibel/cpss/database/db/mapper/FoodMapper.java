package com.seibel.cpss.database.db.mapper;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.database.db.entity.FoodDb;
import com.seibel.cpss.database.db.entity.NutritionDb;
import com.seibel.cpss.database.db.repository.NutritionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class FoodMapper {

    private final NutritionRepository nutritionRepository;
    private final NutritionMapper nutritionMapper;

    public FoodMapper(NutritionRepository nutritionRepository,
                      NutritionMapper nutritionMapper) {
        this.nutritionRepository = nutritionRepository;
        this.nutritionMapper = nutritionMapper;
    }

    public Food toModel(FoodDb item) {
        if (Objects.isNull(item)) {
            return null;
        }

        Food food = new Food();
        food.setExtid(item.getExtid());
        food.setCode(item.getCode());
        food.setName(item.getName());
        food.setCategory(item.getCategory());
        food.setSubcategory(item.getSubcategory());
        food.setDescription(item.getDescription());
        food.setNotes(item.getNotes());
        food.setCrunch(item.getCrunch());
        food.setPunch(item.getPunch());
        food.setSweet(item.getSweet());
        food.setSavory(item.getSavory());
        food.setTypicalServingGrams(item.getTypicalServingGrams());
        food.setFoundation(item.getFoundation());
        food.setMixable(item.getMixable());
        food.setCreatedAt(item.getCreatedAt());
        food.setUpdatedAt(item.getUpdatedAt());
        food.setDeletedAt(item.getDeletedAt());
        food.setActive(item.getActive());

        // Convert entity relationships to domain models
        if (item.getNutrition() != null) {
            food.setNutrition(nutritionMapper.toModel(item.getNutrition()));
        }

        return food;
    }

    public FoodDb toDb(Food item) {
        if (Objects.isNull(item)) {
            return null;
        }

        FoodDb foodDb = new FoodDb();
        foodDb.setExtid(item.getExtid());
        foodDb.setCode(item.getCode());
        foodDb.setName(item.getName());
        foodDb.setCategory(item.getCategory());
        foodDb.setSubcategory(item.getSubcategory());
        foodDb.setDescription(item.getDescription());
        foodDb.setNotes(item.getNotes());
        foodDb.setCrunch(item.getCrunch());
        foodDb.setPunch(item.getPunch());
        foodDb.setSweet(item.getSweet());
        foodDb.setSavory(item.getSavory());
        foodDb.setTypicalServingGrams(item.getTypicalServingGrams());
        foodDb.setFoundation(item.getFoundation());
        foodDb.setMixable(item.getMixable());
        foodDb.setCreatedAt(item.getCreatedAt());
        foodDb.setUpdatedAt(item.getUpdatedAt());
        foodDb.setDeletedAt(item.getDeletedAt());
        foodDb.setActive(item.getActive());

        // Convert domain models to entity relationships (lookup by code)
        if (item.getNutrition() != null) {
            NutritionDb nutritionDb = nutritionRepository.findByCode(item.getNutrition().getCode()).orElse(null);
            foodDb.setNutrition(nutritionDb);
        }

        return foodDb;
    }

    public List<Food> toModelList(List<FoodDb> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<FoodDb> toDbList(List<Food> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toDb).collect(Collectors.toList());
    }
}
