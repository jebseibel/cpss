package com.seibel.cpss.database.db.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "mixture_ingredient")
public class MixtureIngredientDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "mixture_id", referencedColumnName = "id", nullable = false)
    private MixtureDb mixture;

    @ManyToOne
    @JoinColumn(name = "food_id", referencedColumnName = "id", nullable = false)
    private FoodDb food;

    @Column(name = "grams", nullable = false)
    private Integer grams;
}
