package com.seibel.cpss.database.db.entity;

import com.seibel.cpss.common.enums.AssessmentOutcomeEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "food_warning_assessment")
public class FoodWarningAssessmentDb extends BaseDb {

    private static final long serialVersionUID = 22L;

    @Column(name = "food_id", nullable = false)
    private Long foodId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 20, nullable = false)
    private AssessmentOutcomeEnum outcome;

    @Column(name = "reasoning", length = 500)
    private String reasoning;

    @Column(name = "model", length = 50)
    private String model;

    @Column(name = "tool_calls_used")
    private Integer toolCallsUsed;

    @Column(name = "turns_used")
    private Integer turnsUsed;

    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;
}
