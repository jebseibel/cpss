package com.seibel.cpss.database.db.mapper;

import com.seibel.cpss.common.domain.Nutrition;
import com.seibel.cpss.database.db.entity.NutritionDb;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NutritionMapper {

    private final ModelMapper mapper = new ModelMapper();

    public Nutrition toModel(NutritionDb db) {
        return db == null ? null : mapper.map(db, Nutrition.class);
    }

    public NutritionDb toDb(Nutrition model) {
        return model == null ? null : mapper.map(model, NutritionDb.class);
    }

    public List<Nutrition> toModelList(List<NutritionDb> list) {
        return list == null ? List.of() : list.stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<NutritionDb> toDbList(List<Nutrition> list) {
        return list == null ? List.of() : list.stream().map(this::toDb).collect(Collectors.toList());
    }
}
