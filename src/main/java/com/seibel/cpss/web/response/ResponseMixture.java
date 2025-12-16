package com.seibel.cpss.web.response;

import com.seibel.cpss.common.enums.ActiveEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ResponseMixture {
    private String extid;
    private String name;
    private String description;
    private String userExtid;
    private List<MixtureIngredientResponse> ingredients;
    private ResponseNutrition totalNutrition;
    private Integer totalGrams;
    private ActiveEnum active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class MixtureIngredientResponse {
        private String extid;
        private String foodExtid;
        private String foodName;
        private Integer grams;
    }
}
