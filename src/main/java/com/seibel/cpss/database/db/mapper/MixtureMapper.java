package com.seibel.cpss.database.db.mapper;

import com.seibel.cpss.common.domain.Mixture;
import com.seibel.cpss.database.db.entity.MixtureDb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MixtureMapper {

    private final MixtureIngredientMapper ingredientMapper;

    public Mixture toModel(MixtureDb item) {
        if (Objects.isNull(item)) {
            return null;
        }

        Mixture mixture = new Mixture();
        mixture.setId(item.getId());
        mixture.setExtid(item.getExtid());
        mixture.setName(item.getName());
        mixture.setDescription(item.getDescription());
        mixture.setUserExtid(item.getUserExtid());
        mixture.setCreatedAt(item.getCreatedAt());
        mixture.setUpdatedAt(item.getUpdatedAt());
        mixture.setDeletedAt(item.getDeletedAt());
        mixture.setActive(item.getActive());

        // Convert ingredients
        if (item.getIngredients() != null && !item.getIngredients().isEmpty()) {
            mixture.setIngredients(ingredientMapper.toModelList(item.getIngredients()));
        } else {
            mixture.setIngredients(new ArrayList<>());
        }

        return mixture;
    }

    public MixtureDb toDb(Mixture item) {
        if (Objects.isNull(item)) {
            return null;
        }

        MixtureDb mixtureDb = new MixtureDb();
        mixtureDb.setId(item.getId());
        mixtureDb.setExtid(item.getExtid());
        mixtureDb.setName(item.getName());
        mixtureDb.setDescription(item.getDescription());
        mixtureDb.setUserExtid(item.getUserExtid());
        mixtureDb.setCreatedAt(item.getCreatedAt());
        mixtureDb.setUpdatedAt(item.getUpdatedAt());
        mixtureDb.setDeletedAt(item.getDeletedAt());
        mixtureDb.setActive(item.getActive());

        // Note: Ingredients are handled separately in the service layer
        // to avoid circular dependencies and manage relationships properly

        return mixtureDb;
    }

    public List<Mixture> toModelList(List<MixtureDb> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<MixtureDb> toDbList(List<Mixture> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toDb).collect(Collectors.toList());
    }
}
