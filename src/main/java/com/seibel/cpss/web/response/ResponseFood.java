package com.seibel.cpss.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseFood {
    private String extid;
    private String code;
    private String name;
    private String category;
    private String subcategory;
    private String description;
    private String notes;
    private Integer crunch;
    private Integer punch;
    private Integer sweet;
    private Integer savory;
    private ResponseNutrition nutrition;
    private Integer typicalServingGrams;
    private Boolean foundation;
    private Boolean mixable;
}
