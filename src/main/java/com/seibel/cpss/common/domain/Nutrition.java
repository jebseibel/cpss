package com.seibel.cpss.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Nutrition extends BaseDomain {

    private String code;
    private String name;
    private String description;
    private String notes;
    private Integer carbohydrate;
    private Integer fat;
    private Integer protein;
    private Integer sugar;
    private Integer fiber;
    private Integer vitaminD;
    private Integer vitaminE;
}
