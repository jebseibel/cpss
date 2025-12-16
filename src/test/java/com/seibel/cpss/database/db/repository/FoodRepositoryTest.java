package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.FoodDb;
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
class FoodRepositoryTest {

    @Autowired
    private FoodRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnFood_whenExists() {
        // Arrange
        FoodDb food = DomainBuilderDatabase.getFoodDb();
        repository.save(food);

        // Act
        Optional<FoodDb> result = repository.findByExtid(food.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(food.getExtid(), result.get().getExtid());
        assertEquals(food.getCode(), result.get().getCode());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<FoodDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        FoodDb active1 = DomainBuilderDatabase.getFoodDb();
        active1.setActive(ActiveEnum.ACTIVE);
        FoodDb active2 = DomainBuilderDatabase.getFoodDb();
        active2.setActive(ActiveEnum.ACTIVE);
        FoodDb inactive = DomainBuilderDatabase.getFoodDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        List<FoodDb> result = repository.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(f -> f.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        FoodDb food = DomainBuilderDatabase.getFoodDb();
        repository.save(food);

        // Act
        boolean result = repository.existsByExtid(food.getExtid());

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
    void findByCode_shouldReturnFood_whenExists() {
        // Arrange
        FoodDb food = DomainBuilderDatabase.getFoodDb();
        repository.save(food);

        // Act
        Optional<FoodDb> result = repository.findByCode(food.getCode());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(food.getCode(), result.get().getCode());
    }

    @Test
    void findByName_shouldReturnFood_whenExists() {
        // Arrange
        FoodDb food = DomainBuilderDatabase.getFoodDb();
        repository.save(food);

        // Act
        Optional<FoodDb> result = repository.findByName(food.getName());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(food.getName(), result.get().getName());
    }

    @Test
    void save_shouldPersistFood() {
        // Arrange
        FoodDb food = DomainBuilderDatabase.getFoodDb();

        // Act
        FoodDb saved = repository.save(food);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(food.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllFoods() {
        // Arrange
        FoodDb food1 = DomainBuilderDatabase.getFoodDb();
        FoodDb food2 = DomainBuilderDatabase.getFoodDb();
        repository.save(food1);
        repository.save(food2);

        // Act
        List<FoodDb> result = (List<FoodDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }
}
