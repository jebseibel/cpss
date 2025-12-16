package com.seibel.cpss.database.db.mapper;

import com.seibel.cpss.common.domain.MixtureIngredient;
import com.seibel.cpss.database.db.entity.FoodDb;
import com.seibel.cpss.database.db.entity.MixtureDb;
import com.seibel.cpss.database.db.entity.MixtureIngredientDb;
import com.seibel.cpss.database.db.repository.FoodRepository;
import com.seibel.cpss.database.db.repository.MixtureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MixtureIngredientMapper {

    private final FoodMapper foodMapper;
    private final FoodRepository foodRepository;
    private final MixtureRepository mixtureRepository;

    public MixtureIngredient toModel(MixtureIngredientDb item) {
        if (Objects.isNull(item)) {
            return null;
        }

        MixtureIngredient ingredient = new MixtureIngredient();
        ingredient.setId(item.getId());
        ingredient.setExtid(item.getExtid());
        ingredient.setMixtureId(item.getMixture() != null ? item.getMixture().getId() : null);
        ingredient.setFoodExtid(item.getFood() != null ? item.getFood().getExtid() : null);
        ingredient.setFood(item.getFood() != null ? foodMapper.toModel(item.getFood()) : null);
        ingredient.setGrams(item.getGrams());
        ingredient.setCreatedAt(item.getCreatedAt());
        ingredient.setUpdatedAt(item.getUpdatedAt());
        ingredient.setDeletedAt(item.getDeletedAt());
        ingredient.setActive(item.getActive());

        return ingredient;
    }

    public MixtureIngredientDb toDb(MixtureIngredient item) {
        if (Objects.isNull(item)) {
            return null;
        }

        MixtureIngredientDb ingredientDb = new MixtureIngredientDb();
        ingredientDb.setId(item.getId());
        ingredientDb.setExtid(item.getExtid());

        // Set mixture relationship
        if (item.getMixtureId() != null) {
            MixtureDb mixture = mixtureRepository.findById(item.getMixtureId()).orElse(null);
            ingredientDb.setMixture(mixture);
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

    public List<MixtureIngredient> toModelList(List<MixtureIngredientDb> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<MixtureIngredientDb> toDbList(List<MixtureIngredient> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toDb).collect(Collectors.toList());
    }
}
