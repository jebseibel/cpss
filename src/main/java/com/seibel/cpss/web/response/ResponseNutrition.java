package com.seibel.cpss.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseNutrition {

    private String extid;
    private String code;
    private String name;
    private String category;
    private String subcategory;
    private String description;
    private Integer calories;
    private Integer carbohydrate;
    private Integer fat;
    private Integer protein;
    private Integer sugar;
    private Integer fiber;
    private Integer vitaminD;
    private Integer vitaminE;
}

