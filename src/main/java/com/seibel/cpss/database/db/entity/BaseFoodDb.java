package com.seibel.cpss.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Base JPA entity for tables populated via CSV import.
 * Extends BaseDb with common CSV-related fields.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class BaseFoodDb extends BaseDb {

    private static final long serialVersionUID = 2718281828459045235L;

    @Column(name = "code", length = 16, unique = true)
    private String code;

    @Column(name = "name", length = 32, nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "notes")
    private String notes;
}
