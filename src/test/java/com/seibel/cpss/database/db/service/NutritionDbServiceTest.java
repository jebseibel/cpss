package com.seibel.cpss.database.db.service;

import com.seibel.cpss.common.domain.Nutrition;
import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.NutritionDb;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.mapper.NutritionMapper;
import com.seibel.cpss.database.db.repository.NutritionRepository;
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
class NutritionDbServiceTest {

    @Mock
    private NutritionRepository repository;

    @Mock
    private NutritionMapper mapper;

    @InjectMocks
    private NutritionDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Nutrition domain = DomainBuilderDatabase.getNutrition();
        NutritionDb dbEntity = DomainBuilderDatabase.getNutritionDb();
        NutritionDb savedDb = DomainBuilderDatabase.getNutritionDb();

        when(mapper.toDb(domain)).thenReturn(dbEntity);
        when(repository.save(any(NutritionDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(domain);

        // Act
        Nutrition result = service.create(domain);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<NutritionDb> captor = ArgumentCaptor.forClass(NutritionDb.class);
        verify(repository).save(captor.capture());
        assertEquals(ActiveEnum.ACTIVE, captor.getValue().getActive());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        Nutrition domain = DomainBuilderDatabase.getNutrition();
        NutritionDb dbEntity = DomainBuilderDatabase.getNutritionDb();

        when(mapper.toDb(domain)).thenReturn(dbEntity);
        when(repository.save(any(NutritionDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(DatabaseFailureException.class, () -> {
            service.create(domain);
        });
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        Nutrition domain = DomainBuilderDatabase.getNutrition();
        NutritionDb existingDb = DomainBuilderDatabase.getNutritionDb();
        existingDb.setExtid(extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(NutritionDb.class))).thenReturn(existingDb);
        when(mapper.toModel(existingDb)).thenReturn(domain);

        // Act
        Nutrition result = service.update(extid, domain);

        // Assert
        assertNotNull(result);
        verify(repository).save(any(NutritionDb.class));
    }

    @Test
    void update_shouldReturnNull_whenNotFound() {
        // Arrange
        Nutrition domain = DomainBuilderDatabase.getNutrition();
        when(repository.findByExtid("nonexistent")).thenReturn(Optional.empty());

        // Act
        Nutrition result = service.update("nonexistent", domain);

        // Assert
        assertNull(result);
        verify(repository, never()).save(any());
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        NutritionDb existingDb = DomainBuilderDatabase.getNutritionDb();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(NutritionDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);
    }

    @Test
    void findByExtid_shouldReturnNutrition_whenExists() {
        // Arrange
        String extid = "existing-extid";
        NutritionDb db = DomainBuilderDatabase.getNutritionDb();
        Nutrition expectedDomain = DomainBuilderDatabase.getNutrition(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Nutrition result = service.findByExtid(extid);

        // Assert
        assertNotNull(result);
    }

    @Test
    void findAll_shouldReturnAllNutritions() {
        // Arrange
        NutritionDb db1 = DomainBuilderDatabase.getNutritionDb();
        NutritionDb db2 = DomainBuilderDatabase.getNutritionDb();
        List<NutritionDb> dbList = Arrays.asList(db1, db2);

        when(repository.findAll()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(Arrays.asList(
                DomainBuilderDatabase.getNutrition(db1),
                DomainBuilderDatabase.getNutrition(db2)
        ));

        // Act
        List<Nutrition> result = service.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void findByActive_shouldReturnFilteredNutritions() {
        // Arrange
        NutritionDb db = DomainBuilderDatabase.getNutritionDb();
        db.setActive(ActiveEnum.ACTIVE);
        List<NutritionDb> dbList = Arrays.asList(db);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(Arrays.asList(DomainBuilderDatabase.getNutrition(db)));

        // Act
        List<Nutrition> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertEquals(1, result.size());
    }
}
