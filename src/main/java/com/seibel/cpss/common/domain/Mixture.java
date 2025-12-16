package com.seibel.cpss.common.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Mixture extends BaseDomain {
    private String name;
    private String description;
    private String userExtid;  // Reference to the user who created it

    @Builder.Default
    private List<MixtureIngredient> ingredients = new ArrayList<>();
}
