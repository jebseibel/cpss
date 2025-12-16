package com.seibel.cpss.database.db.mapper;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.database.db.entity.FoodDb;
import com.seibel.cpss.database.db.entity.NutritionDb;
import com.seibel.cpss.database.db.repository.NutritionRepository;
import com.seibel.cpss.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class FoodMapperTest {

    @Mock
    private NutritionRepository nutritionRepository;
    @Mock
    private NutritionMapper nutritionMapper;

    private FoodMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FoodMapper(nutritionRepository, nutritionMapper);

        // Setup default mock behavior with lenient stubbings
        NutritionDb nutritionDb = DomainBuilderDatabase.getNutritionDb();

        org.mockito.Mockito.lenient().when(nutritionRepository.findByCode(anyString())).thenReturn(Optional.of(nutritionDb));
        org.mockito.Mockito.lenient().when(nutritionMapper.toModel(nutritionDb)).thenReturn(DomainBuilderDatabase.getNutrition(nutritionDb));
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        FoodDb db = DomainBuilderDatabase.getFoodDb();

        // Act
        Food domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getCode(), domain.getCode());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getCategory(), domain.getCategory());
        assertEquals(db.getSubcategory(), domain.getSubcategory());
        assertEquals(db.getDescription(), domain.getDescription());
        assertEquals(db.getNotes(), domain.getNotes());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Food domain = DomainBuilderDatabase.getFood();

        // Act
        FoodDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getCode(), db.getCode());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getCategory(), db.getCategory());
        assertEquals(domain.getSubcategory(), db.getSubcategory());
        assertEquals(domain.getDescription(), db.getDescription());
        assertEquals(domain.getNotes(), db.getNotes());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        FoodDb db1 = DomainBuilderDatabase.getFoodDb();
        FoodDb db2 = DomainBuilderDatabase.getFoodDb();
        List<FoodDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Food> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<FoodDb> dbList = Arrays.asList();

        // Act
        List<Food> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Food> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Food domain1 = DomainBuilderDatabase.getFood();
        Food domain2 = DomainBuilderDatabase.getFood();
        List<Food> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<FoodDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Food> domainList = Arrays.asList();

        // Act
        List<FoodDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<FoodDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
