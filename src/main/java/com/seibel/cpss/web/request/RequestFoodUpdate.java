package com.seibel.cpss.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RequestFoodUpdate extends BaseRequest {

    @Size(max = 8)
    private String code;

    @Size(max = 32)
    private String name;

    @Size(max = 32)
    private String category;

    @Size(max = 32)
    private String subcategory;

    @Size(max = 255)
    private String description;

    @Size(max = 1000)
    private String notes;

    private Integer crunch;
    private Integer punch;
    private Integer sweet;
    private Integer savory;
    private String nutritionExtid;
    private Integer typicalServingGrams;
    private Boolean foundation;
    private Boolean mixable;
}
