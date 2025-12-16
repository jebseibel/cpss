package com.seibel.cpss.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "nutrition")
public class NutritionDb extends BaseFoodDb {

    private static final long serialVersionUID = 6732009184148857729L;

    @Column(name = "carbohydrate", nullable = false)
    private Integer carbohydrate;

    @Column(name = "fat", nullable = false)
    private Integer fat;

    @Column(name = "protein", nullable = false)
    private Integer protein;

    @Column(name = "sugar", nullable = false)
    private Integer sugar;

    @Column(name = "fiber")
    private Integer fiber;

    @Column(name = "vitamin_d")
    private Integer vitaminD;

    @Column(name = "vitamin_e")
    private Integer vitaminE;
}

