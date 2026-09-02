package com.seibel.cpss.database.db.mapper;

import com.seibel.cpss.common.domain.FoodWarningAssessment;
import com.seibel.cpss.database.db.entity.FoodWarningAssessmentDb;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor
public class FoodWarningAssessmentMapper {

    public FoodWarningAssessment toModel(FoodWarningAssessmentDb item) {
        if (Objects.isNull(item)) {
            return null;
        }

        FoodWarningAssessment assessment = new FoodWarningAssessment();
        assessment.setId(item.getId());
        assessment.setExtid(item.getExtid());
        assessment.setFoodId(item.getFoodId());
        assessment.setOutcome(item.getOutcome());
        assessment.setReasoning(item.getReasoning());
        assessment.setModel(item.getModel());
        assessment.setToolCallsUsed(item.getToolCallsUsed());
        assessment.setTurnsUsed(item.getTurnsUsed());
        assessment.setAssessedAt(item.getAssessedAt());
        assessment.setCreatedAt(item.getCreatedAt());
        assessment.setUpdatedAt(item.getUpdatedAt());
        assessment.setDeletedAt(item.getDeletedAt());
        assessment.setActive(item.getActive());

        return assessment;
    }

    public FoodWarningAssessmentDb toDb(FoodWarningAssessment item) {
        if (Objects.isNull(item)) {
            return null;
        }

        FoodWarningAssessmentDb db = new FoodWarningAssessmentDb();
        db.setId(item.getId());
        db.setExtid(item.getExtid());
        db.setFoodId(item.getFoodId());
        db.setOutcome(item.getOutcome());
        db.setReasoning(item.getReasoning());
        db.setModel(item.getModel());
        db.setToolCallsUsed(item.getToolCallsUsed());
        db.setTurnsUsed(item.getTurnsUsed());
        db.setAssessedAt(item.getAssessedAt());
        db.setCreatedAt(item.getCreatedAt());
        db.setUpdatedAt(item.getUpdatedAt());
        db.setDeletedAt(item.getDeletedAt());
        db.setActive(item.getActive());

        return db;
    }

    public List<FoodWarningAssessment> toModelList(List<FoodWarningAssessmentDb> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toModel).collect(Collectors.toList());
    }

    public List<FoodWarningAssessmentDb> toDbList(List<FoodWarningAssessment> items) {
        return Objects.isNull(items) ? List.of() :
                items.stream().map(this::toDb).collect(Collectors.toList());
    }
}
