package com.seibel.cpss.common.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The concerns a food warning can belong to. Deliberately a closed set — the
 * triage and research prompts are given these names and nothing else, so the
 * model classifies into a fixed vocabulary rather than inventing categories
 * that would fragment the catalog.
 */
public enum WarningCategoryEnum {
    OXALATE("oxalate"),
    VITAMIN_K("vitamin_k"),
    ALLERGEN("allergen"),
    GLYCEMIC("glycemic"),
    PURINE("purine"),
    TYRAMINE("tyramine"),
    SOLANINE("solanine"),
    RAW_RISK("raw_risk"),
    INTERACTION("interaction");

    public final String value;

    WarningCategoryEnum(String value) {
        this.value = value;
    }

    public static WarningCategoryEnum fromValue(String value) {
        return Arrays.stream(values())
                .filter(c -> c.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }

    public static final String ALLOWED_VALUES =
            Arrays.stream(values())
                    .map(c -> c.value)
                    .collect(Collectors.joining(", "));
}
