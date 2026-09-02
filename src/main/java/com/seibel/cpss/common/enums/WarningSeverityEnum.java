package com.seibel.cpss.common.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * How strongly a warning is phrased. AVOID_IF always names the population it
 * applies to ("if taking warfarin") — this app issues no blanket medical
 * instructions.
 */
public enum WarningSeverityEnum {
    INFO("info"),
    CAUTION("caution"),
    AVOID_IF("avoid_if");

    public final String value;

    WarningSeverityEnum(String value) {
        this.value = value;
    }

    public static WarningSeverityEnum fromValue(String value) {
        return Arrays.stream(values())
                .filter(s -> s.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }

    public static final String ALLOWED_VALUES =
            Arrays.stream(values())
                    .map(s -> s.value)
                    .collect(Collectors.joining(", "));
}
