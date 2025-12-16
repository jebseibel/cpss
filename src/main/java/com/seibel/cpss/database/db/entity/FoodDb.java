package com.seibel.cpss.database.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "food")
public class FoodDb extends BaseFoodDb {

    private static final long serialVersionUID = 4913572206441095325L;

    @Column(name = "category", length = 32, nullable = false)
    private String category;

    @Column(name = "subcategory", length = 32, nullable = false)
    private String subcategory;

    @Min(1)
    @Max(5)
    @Column(name = "crunch")
    private Integer crunch;

    @Min(1)
    @Max(5)
    @Column(name = "punch")
    private Integer punch;

    @Min(1)
    @Max(5)
    @Column(name = "sweet")
    private Integer sweet;

    @Min(1)
    @Max(5)
    @Column(name = "savory")
    private Integer savory;

    @OneToOne
    @JoinColumn(name = "nutrition_id", referencedColumnName = "id")
    private NutritionDb nutrition;

    @Column(name = "typical_serving_grams")
    private Integer typicalServingGrams;

    @Column(name = "foundation", nullable = false)
    private Boolean foundation = false;

    @Column(name = "mixable", nullable = false)
    private Boolean mixable = false;
}

