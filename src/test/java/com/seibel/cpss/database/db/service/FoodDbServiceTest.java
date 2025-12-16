package com.seibel.cpss.database.db.service;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.FoodDb;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.mapper.FoodMapper;
import com.seibel.cpss.database.db.mapper.NutritionMapper;
import com.seibel.cpss.database.db.repository.FoodRepository;
import com.seibel.cpss.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodDbServiceTest {

    @Mock
    private FoodRepository repository;

    @Mock
    private FoodMapper mapper;

    @Mock
    private NutritionMapper nutritionMapper;

    @InjectMocks
    private FoodDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Food domain = DomainBuilderDatabase.getFood();
        FoodDb dbEntity = DomainBuilderDatabase.getFoodDb();
        FoodDb savedDb = DomainBuilderDatabase.getFoodDb();

        when(mapper.toDb(domain)).thenReturn(dbEntity);
        when(repository.save(any(FoodDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(domain);

        // Act
        Food result = service.create(domain);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<FoodDb> captor = ArgumentCaptor.forClass(FoodDb.class);
        verify(repository).save(captor.capture());
        assertEquals(ActiveEnum.ACTIVE, captor.getValue().getActive());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        Food domain = DomainBuilderDatabase.getFood();
        FoodDb dbEntity = DomainBuilderDatabase.getFoodDb();

        when(mapper.toDb(domain)).thenReturn(dbEntity);
        when(repository.save(any(FoodDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(DatabaseFailureException.class, () -> {
            service.create(domain);
        });
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        Food domain = DomainBuilderDatabase.getFood();
        FoodDb existingDb = DomainBuilderDatabase.getFoodDb();
        existingDb.setExtid(extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FoodDb.class))).thenReturn(existingDb);
        when(mapper.toModel(existingDb)).thenReturn(domain);

        // Act
        Food result = service.update(extid, domain);

        // Assert
        assertNotNull(result);
        verify(repository).save(any(FoodDb.class));
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        Food domain = DomainBuilderDatabase.getFood();
        when(repository.findByExtid("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DatabaseFailureException.class, () -> {
            service.update("nonexistent", domain);
        });
        verify(repository, never()).save(any());
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        FoodDb existingDb = DomainBuilderDatabase.getFoodDb();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FoodDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);
    }

    @Test
    void findByExtid_shouldReturnFood_whenExists() {
        // Arrange
        String extid = "existing-extid";
        FoodDb db = DomainBuilderDatabase.getFoodDb();
        Food expectedDomain = DomainBuilderDatabase.getFood(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Food result = service.findByExtid(extid);

        // Assert
        assertNotNull(result);
    }

    @Test
    void findAll_shouldReturnAllFoods() {
        // Arrange
        FoodDb db1 = DomainBuilderDatabase.getFoodDb();
        FoodDb db2 = DomainBuilderDatabase.getFoodDb();
        List<FoodDb> dbList = Arrays.asList(db1, db2);

        when(repository.findAll()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(Arrays.asList(
                DomainBuilderDatabase.getFood(db1),
                DomainBuilderDatabase.getFood(db2)
        ));

        // Act
        List<Food> result = service.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void findByActive_shouldReturnFilteredFoods() {
        // Arrange
        FoodDb db = DomainBuilderDatabase.getFoodDb();
        db.setActive(ActiveEnum.ACTIVE);
        List<FoodDb> dbList = Arrays.asList(db);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(Arrays.asList(DomainBuilderDatabase.getFood(db)));

        // Act
        List<Food> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertEquals(1, result.size());
    }
}
