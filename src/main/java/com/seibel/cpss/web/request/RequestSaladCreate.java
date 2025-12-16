package com.seibel.cpss.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RequestSaladCreate extends BaseRequest {

    @NotEmpty(message = "The name is required.")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotEmpty(message = "At least one ingredient is required.")
    private List<SaladIngredientRequest> foodIngredients;

    @Data
    public static class SaladIngredientRequest {
        @NotEmpty(message = "Food extid is required.")
        private String foodExtid;

        @NotNull(message = "Grams is required.")
        private Integer grams;
    }
}
