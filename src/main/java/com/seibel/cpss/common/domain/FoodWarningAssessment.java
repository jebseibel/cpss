package com.seibel.cpss.common.domain;

import com.seibel.cpss.common.enums.AssessmentOutcomeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * A record that the agent examined one food, including the case where it
 * decided nothing was needed. Lets a re-run skip work already done, and makes
 * "we checked cucumber and it was fine" a stored fact rather than an inference
 * from the absence of a warning.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FoodWarningAssessment extends BaseDomain {

    private Long foodId;
    private AssessmentOutcomeEnum outcome;
    private String reasoning;
    private String model;

    /** Loop instrumentation — drives budget tuning from data, not guesswork. */
    private Integer toolCallsUsed;
    private Integer turnsUsed;

    private LocalDateTime assessedAt;
}
