package com.seibel.cpss.database.db.mapper;

import com.seibel.cpss.common.domain.SaladFoodIngredient;
import com.seibel.cpss.database.db.entity.FoodDb;
import com.seibel.cpss.database.db.entity.SaladDb;
import com.seibel.cpss.database.db.entity.SaladFoodIngredientDb;
import com.seibel.cpss.database.db.repository.FoodRepository;
import com.seibel.cpss.database.db.repository.SaladRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SaladFoodIngredientMapper {

    private final FoodMapper foodMapper;
    private final FoodRepository foodRepository;
    private final SaladRepository saladRepository;

    public SaladFoodIngredient toModel(SaladFoodIngredientDb item) {
        if (Objects.isNull(item)) {
            return null;
        }

        SaladFoodIngredient ingredient = new SaladFoodIngredient();
        ingredient.setId(item.getId());
        ingredient.setExtid(item.getExtid());
        ingredient.setSaladId(item.getSalad() != null ? item.getSalad().getId() : null);
        ingredient.setFoodExtid(item.getFood() != null ? item.getFood().getExtid() : null);
        ingredient.setFood(item.getFood() != null ? foodMapper.toModel(item.getFood()) : null);
        ingredient.setGrams(item.getGrams());
        ingredient.setCreatedAt(item.getCreatedAt());
        ingredient.setUpdatedAt(item.getUpdatedAt());
        ingredient.setDeletedAt(item.getDeletedAt());
        ingredient.setActive(item.getActive());

        return ingredient;
    }

    public SaladFoodIngredientDb toDb(SaladFoodIngredient item) {
        if (Objects.isNull(item)) {
            return null;
        }

        SaladFoodIngredientDb ingredientDb = new SaladFoodIngredientDb();
        ingredientDb.setId(item.getId());
        ingredientDb.setExtid(item.getExtid());

        // Set salad relationship
        if (item.getSaladId() != null) {
            SaladDb salad = saladRepository.findById(item.getSaladId()).orElse(null);
            ingredientDb.setSalad(salad);
        }

        // Set food relationship using foodExtid
        if (item.getFoodExtid() != null) {
            FoodDb food = foodRepository.findByExtid(item.getFoodExtid()).orElse(null);
            ingredientDb.setFood(food);
        }

        ingredientDb.setGrams(item.getGrams());
        ingredientDb.setCreatedAt(item.getCreatedAt());
        ingredientDb.setUpdatedAt(item.getUpdatedAt());
        ingredientDb.setDeletedAt(item.getDeletedAt());
        ingredientDb.setActive(item.getActive());

        return ingredientDb;
    }

    public List<SaladFoodIngredient> toModelList(List<SaladFoodIngredientDb> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<SaladFoodIngredientDb> toDbList(List<SaladFoodIngredient> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toDb).collect(Collectors.toList());
    }
}
