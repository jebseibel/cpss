package com.seibel.cpss.database.db.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "salad_food_ingredient")
public class SaladFoodIngredientDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "salad_id", referencedColumnName = "id", nullable = false)
    private SaladDb salad;

    @ManyToOne
    @JoinColumn(name = "food_id", referencedColumnName = "id", nullable = false)
    private FoodDb food;

    @Column(name = "grams", nullable = false)
    private Integer grams;
}
