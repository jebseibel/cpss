package com.seibel.cpss.common.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * What happened when the agent looked at one food.
 *
 * <p>WITHDRAWN and BUDGET_EXHAUSTED are separate on purpose. The first is the
 * research loop working correctly — it started from a suspicion and disproved
 * it. The second is a food that needs a human or a bigger tool-call budget.
 * Collapsing them would hide the signal that tells you whether the budget is
 * set right.
 */
public enum AssessmentOutcomeEnum {
    /** Triage decided no research was warranted. The common case. */
    NO_WARNING("no_warning"),
    /** Research ran and concluded the suspected concern does not hold. */
    WITHDRAWN("withdrawn"),
    /** Research produced a sourced warning, now pending review. */
    WARNING_DRAFTED("warning_drafted"),
    /** Loop hit its tool-call or turn limit without reaching a conclusion. */
    BUDGET_EXHAUSTED("budget_exhausted"),
    /** Tool or API errors prevented a conclusion. */
    RESEARCH_FAILED("research_failed");

    public final String value;

    AssessmentOutcomeEnum(String value) {
        this.value = value;
    }

    public static AssessmentOutcomeEnum fromValue(String value) {
        return Arrays.stream(values())
                .filter(o -> o.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }

    public static final String ALLOWED_VALUES =
            Arrays.stream(values())
                    .map(o -> o.value)
                    .collect(Collectors.joining(", "));
}
