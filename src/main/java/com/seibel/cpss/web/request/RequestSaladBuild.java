package com.seibel.cpss.web.request;

import lombok.Data;

import java.util.List;

@Data
public class RequestSaladBuild {
    private List<SaladIngredient> ingredients;

    @Data
    public static class SaladIngredient {
        private String foodExtid;
        private Double quantity; // multiplier for serving size
    }
}
