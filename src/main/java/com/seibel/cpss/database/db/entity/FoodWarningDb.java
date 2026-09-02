package com.seibel.cpss.database.db.entity;

import com.seibel.cpss.common.enums.WarningCategoryEnum;
import com.seibel.cpss.common.enums.WarningSeverityEnum;
import com.seibel.cpss.common.enums.WarningStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "food_warning")
public class FoodWarningDb extends BaseDb {

    private static final long serialVersionUID = 21L;

    @Column(name = "food_id", nullable = false)
    private Long foodId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30, nullable = false)
    private WarningCategoryEnum category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20, nullable = false)
    private WarningSeverityEnum severity;

    @Column(name = "applies_to", length = 200)
    private String appliesTo;

    @Column(name = "warning_text", length = 500, nullable = false)
    private String warningText;

    @Column(name = "sources", columnDefinition = "text")
    private String sources;

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private WarningStatusEnum status;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
