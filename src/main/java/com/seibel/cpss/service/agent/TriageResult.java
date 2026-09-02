package com.seibel.cpss.service.agent;

import com.seibel.cpss.common.enums.WarningCategoryEnum;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Output of the triage classifier. Not an agent — one call, fixed shape, no
 * tools. Its only job is to decide whether a food is worth the research loop.
 */
@Data
@Builder
public class TriageResult {

    private boolean needsResearch;
    private List<WarningCategoryEnum> suspectedCategories;
    private String reasoning;

    /** True when the classifier failed and we could not get a usable answer. */
    private boolean failed;

    public static TriageResult abstain(String reasoning) {
        return TriageResult.builder()
                .needsResearch(false)
                .suspectedCategories(List.of())
                .reasoning(reasoning)
                .build();
    }

    public static TriageResult failure(String reasoning) {
        return TriageResult.builder()
                .needsResearch(false)
                .suspectedCategories(List.of())
                .reasoning(reasoning)
                .failed(true)
                .build();
    }
}
