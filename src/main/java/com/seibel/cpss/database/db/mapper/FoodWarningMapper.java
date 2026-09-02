package com.seibel.cpss.database.db.mapper;

import com.seibel.cpss.common.domain.FoodWarning;
import com.seibel.cpss.database.db.entity.FoodWarningDb;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor
public class FoodWarningMapper {

    public FoodWarning toModel(FoodWarningDb item) {
        if (Objects.isNull(item)) {
            return null;
        }

        FoodWarning warning = new FoodWarning();
        warning.setId(item.getId());
        warning.setExtid(item.getExtid());
        warning.setFoodId(item.getFoodId());
        warning.setCategory(item.getCategory());
        warning.setSeverity(item.getSeverity());
        warning.setAppliesTo(item.getAppliesTo());
        warning.setWarningText(item.getWarningText());
        warning.setSources(item.getSources());
        warning.setConfidence(item.getConfidence());
        warning.setStatus(item.getStatus());
        warning.setReviewedBy(item.getReviewedBy());
        warning.setReviewedAt(item.getReviewedAt());
        warning.setCreatedAt(item.getCreatedAt());
        warning.setUpdatedAt(item.getUpdatedAt());
        warning.setDeletedAt(item.getDeletedAt());
        warning.setActive(item.getActive());

        return warning;
    }

    public FoodWarningDb toDb(FoodWarning item) {
        if (Objects.isNull(item)) {
            return null;
        }

        FoodWarningDb db = new FoodWarningDb();
        db.setId(item.getId());
        db.setExtid(item.getExtid());
        db.setFoodId(item.getFoodId());
        db.setCategory(item.getCategory());
        db.setSeverity(item.getSeverity());
        db.setAppliesTo(item.getAppliesTo());
        db.setWarningText(item.getWarningText());
        db.setSources(item.getSources());
        db.setConfidence(item.getConfidence());
        db.setStatus(item.getStatus());
        db.setReviewedBy(item.getReviewedBy());
        db.setReviewedAt(item.getReviewedAt());
        db.setCreatedAt(item.getCreatedAt());
        db.setUpdatedAt(item.getUpdatedAt());
        db.setDeletedAt(item.getDeletedAt());
        db.setActive(item.getActive());

        return db;
    }

    public List<FoodWarning> toModelList(List<FoodWarningDb> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<FoodWarningDb> toDbList(List<FoodWarning> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toDb).collect(Collectors.toList());
    }
}
