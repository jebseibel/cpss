package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.NutritionDb;
import com.seibel.cpss.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NutritionRepositoryTest {

    @Autowired
    private NutritionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnNutrition_whenExists() {
        // Arrange
        NutritionDb nutrition = DomainBuilderDatabase.getNutritionDb();
        repository.save(nutrition);

        // Act
        Optional<NutritionDb> result = repository.findByExtid(nutrition.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(nutrition.getExtid(), result.get().getExtid());
        assertEquals(nutrition.getCode(), result.get().getCode());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<NutritionDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        NutritionDb active1 = DomainBuilderDatabase.getNutritionDb();
        active1.setActive(ActiveEnum.ACTIVE);
        NutritionDb active2 = DomainBuilderDatabase.getNutritionDb();
        active2.setActive(ActiveEnum.ACTIVE);
        NutritionDb inactive = DomainBuilderDatabase.getNutritionDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        List<NutritionDb> result = repository.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(n -> n.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        NutritionDb nutrition = DomainBuilderDatabase.getNutritionDb();
        repository.save(nutrition);

        // Act
        boolean result = repository.existsByExtid(nutrition.getExtid());

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotExists() {
        // Act
        boolean result = repository.existsByExtid("nonexistent-extid");

        // Assert
        assertFalse(result);
    }

    @Test
    void findByCode_shouldReturnNutrition_whenExists() {
        // Arrange
        NutritionDb nutrition = DomainBuilderDatabase.getNutritionDb();
        repository.save(nutrition);

        // Act
        Optional<NutritionDb> result = repository.findByCode(nutrition.getCode());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(nutrition.getCode(), result.get().getCode());
    }

    @Test
    void findByName_shouldReturnNutrition_whenExists() {
        // Arrange
        NutritionDb nutrition = DomainBuilderDatabase.getNutritionDb();
        repository.save(nutrition);

        // Act
        Optional<NutritionDb> result = repository.findByName(nutrition.getName());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(nutrition.getName(), result.get().getName());
    }

    @Test
    void save_shouldPersistNutrition() {
        // Arrange
        NutritionDb nutrition = DomainBuilderDatabase.getNutritionDb();

        // Act
        NutritionDb saved = repository.save(nutrition);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(nutrition.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllNutritions() {
        // Arrange
        NutritionDb nutrition1 = DomainBuilderDatabase.getNutritionDb();
        NutritionDb nutrition2 = DomainBuilderDatabase.getNutritionDb();
        repository.save(nutrition1);
        repository.save(nutrition2);

        // Act
        List<NutritionDb> result = (List<NutritionDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }
}
