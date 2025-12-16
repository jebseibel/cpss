package com.seibel.cpss.database.db.mapper;

import com.seibel.cpss.common.domain.Nutrition;
import com.seibel.cpss.database.db.entity.NutritionDb;
import com.seibel.cpss.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NutritionMapperTest {

    private NutritionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NutritionMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        NutritionDb db = DomainBuilderDatabase.getNutritionDb();

        // Act
        Nutrition domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getCode(), domain.getCode());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getDescription(), domain.getDescription());
        assertEquals(db.getCarbohydrate(), domain.getCarbohydrate());
        assertEquals(db.getFat(), domain.getFat());
        assertEquals(db.getProtein(), domain.getProtein());
        assertEquals(db.getSugar(), domain.getSugar());
        assertEquals(db.getFiber(), domain.getFiber());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Nutrition domain = DomainBuilderDatabase.getNutrition();

        // Act
        NutritionDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getCode(), db.getCode());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getDescription(), db.getDescription());
        assertEquals(domain.getCarbohydrate(), db.getCarbohydrate());
        assertEquals(domain.getFat(), db.getFat());
        assertEquals(domain.getProtein(), db.getProtein());
        assertEquals(domain.getSugar(), db.getSugar());
        assertEquals(domain.getFiber(), db.getFiber());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        NutritionDb db1 = DomainBuilderDatabase.getNutritionDb();
        NutritionDb db2 = DomainBuilderDatabase.getNutritionDb();
        List<NutritionDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Nutrition> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<NutritionDb> dbList = Arrays.asList();

        // Act
        List<Nutrition> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Nutrition> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Nutrition domain1 = DomainBuilderDatabase.getNutrition();
        Nutrition domain2 = DomainBuilderDatabase.getNutrition();
        List<Nutrition> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<NutritionDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Nutrition> domainList = Arrays.asList();

        // Act
        List<NutritionDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<NutritionDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
