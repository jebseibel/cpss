package com.seibel.cpss.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RequestSaladUpdate extends BaseRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private List<SaladIngredientRequest> foodIngredients;

    @Data
    public static class SaladIngredientRequest {
        private String foodExtid;
        private Integer grams;
    }
}
