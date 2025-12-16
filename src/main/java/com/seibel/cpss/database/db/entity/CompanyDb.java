package com.seibel.cpss.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "company")
public class CompanyDb extends BaseDb {

    private static final long serialVersionUID = 330515747211210728L;

    @Column(name = "code", length = 16, unique = true)
    private String code;

    @Column(name = "name", length = 32, nullable = false, unique = true)
    private String name;

    @Column(name = "description", length = 255, nullable = false)
    private String description;
}
