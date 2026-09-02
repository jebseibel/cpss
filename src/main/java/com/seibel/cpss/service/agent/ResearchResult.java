package com.seibel.cpss.service.agent;

import com.seibel.cpss.common.enums.AssessmentOutcomeEnum;
import com.seibel.cpss.common.enums.WarningCategoryEnum;
import com.seibel.cpss.common.enums.WarningSeverityEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Outcome of one run of the research loop over a single food.
 *
 * <p>{@code outcome} distinguishes the loop concluding "no warning needed"
 * (WITHDRAWN — it worked) from the loop running out of budget
 * (BUDGET_EXHAUSTED — it needs a human or a bigger allowance). Both produce no
 * warning, but they mean opposite things about the health of the system.
 */
@Data
@Builder
public class ResearchResult {

    private AssessmentOutcomeEnum outcome;

    private WarningCategoryEnum category;
    private WarningSeverityEnum severity;
    private String appliesTo;
    private String warningText;
    private String sources;
    private BigDecimal confidence;

    private String reasoning;

    /** Loop instrumentation — how much budget this food actually consumed. */
    private int toolCallsUsed;
    private int turnsUsed;

    public boolean hasWarning() {
        return outcome == AssessmentOutcomeEnum.WARNING_DRAFTED;
    }
}
