package com.seibel.cpss.database.db.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "mixture")
public class MixtureDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "user_extid", length = 36)
    private String userExtid;

    @OneToMany(mappedBy = "mixture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MixtureIngredientDb> ingredients = new ArrayList<>();
}
