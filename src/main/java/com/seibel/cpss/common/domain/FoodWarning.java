package com.seibel.cpss.common.domain;

import com.seibel.cpss.common.enums.WarningCategoryEnum;
import com.seibel.cpss.common.enums.WarningSeverityEnum;
import com.seibel.cpss.common.enums.WarningStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A health note attached to a single food. Drafted by the food-warning agent,
 * never served to users until a human approves it.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FoodWarning extends BaseDomain {

    private Long foodId;
    private WarningCategoryEnum category;
    private WarningSeverityEnum severity;

    /** Who the warning concerns, e.g. "people taking warfarin". Null for INFO. */
    private String appliesTo;

    private String warningText;

    /** JSON array of {title, url} — every factual claim must cite one. */
    private String sources;

    private BigDecimal confidence;

    private WarningStatusEnum status;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
}
