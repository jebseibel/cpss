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
public class SaladFoodIngredient extends BaseDomain {
    private Long saladId;
    private String foodExtid;
    private Food food;
    private Integer grams;
}
