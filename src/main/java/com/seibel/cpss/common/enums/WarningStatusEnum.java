package com.seibel.cpss.common.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Review lifecycle for an agent-drafted warning. The agent only ever writes
 * PENDING; a human moves it to APPROVED or REJECTED. Only APPROVED rows are
 * served by the API.
 */
public enum WarningStatusEnum {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    public final String value;

    WarningStatusEnum(String value) {
        this.value = value;
    }

    public static WarningStatusEnum fromValue(String value) {
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
