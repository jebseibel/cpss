package com.seibel.cpss.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RequestNutritionCreate extends BaseRequest {

    @NotEmpty(message = "The code is required.")
    @Size(max = 8)
    private String code;

    @NotEmpty(message = "The name is required.")
    @Size(max = 32)
    private String name;

    @NotEmpty(message = "The category is required.")
    @Size(max = 32)
    private String category;

    @NotEmpty(message = "The subcategory is required.")
    @Size(max = 32)
    private String subcategory;

    @Size(max = 255)
    private String description;

    @NotNull(message = "The carbohydrate value is required.")
    private Integer carbohydrate;

    @NotNull(message = "The fat value is required.")
    private Integer fat;

    @NotNull(message = "The protein value is required.")
    private Integer protein;

    @NotNull(message = "The sugar value is required.")
    private Integer sugar;

    private Integer vitaminD;
    private Integer vitaminE;
}
