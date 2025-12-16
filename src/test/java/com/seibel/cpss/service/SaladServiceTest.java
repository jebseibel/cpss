package com.seibel.cpss.service;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.Salad;
import com.seibel.cpss.common.domain.SaladFoodIngredient;
import com.seibel.cpss.common.exceptions.ValidationException;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.service.SaladDbService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaladServiceTest {

    @Mock
    private SaladDbService dbService;

    @Mock
    private FoodService foodService;

    @InjectMocks
    private SaladService service;

    @Test
    void create_shouldSucceed_withOneFoundationIngredient() throws DatabaseFailureException {
        // Arrange
        Salad salad = createTestSalad();
        salad.setFoodIngredients(List.of(
            createIngredient("food-1", true)  // 1 foundation
        ));

        when(foodService.findByExtidIn(anyList())).thenReturn(List.of(
            createFood("food-1", true)
        ));
        when(dbService.create(any(Salad.class))).thenReturn(salad);

        // Act
        Salad result = service.create(salad);

        // Assert
        assertNotNull(result);
        verify(dbService).create(salad);
    }

    @Test
    void create_shouldSucceed_withTwoFoundationIngredients() throws DatabaseFailureException {
        // Arrange
        Salad salad = createTestSalad();
        salad.setFoodIngredients(List.of(
            createIngredient("food-1", true),   // 1 foundation
            createIngredient("food-2", true),   // 2 foundation
            createIngredient("food-3", false)   // not foundation
        ));

        when(foodService.findByExtidIn(anyList())).thenReturn(List.of(
            createFood("food-1", true),
            createFood("food-2", true),
            createFood("food-3", false)
        ));
        when(dbService.create(any(Salad.class))).thenReturn(salad);

        // Act
        Salad result = service.create(salad);

        // Assert
        assertNotNull(result);
        verify(dbService).create(salad);
    }

    @Test
    void create_shouldSucceed_withThreeFoundationIngredients() throws DatabaseFailureException {
        // Arrange
        Salad salad = createTestSalad();
        salad.setFoodIngredients(List.of(
            createIngredient("food-1", true),   // 1 foundation
            createIngredient("food-2", true),   // 2 foundation
            createIngredient("food-3", true),   // 3 foundation
            createIngredient("food-4", false)   // not foundation
        ));

        when(foodService.findByExtidIn(anyList())).thenReturn(List.of(
            createFood("food-1", true),
            createFood("food-2", true),
            createFood("food-3", true),
            createFood("food-4", false)
        ));
        when(dbService.create(any(Salad.class))).thenReturn(salad);

        // Act
        Salad result = service.create(salad);

        // Assert
        assertNotNull(result);
        verify(dbService).create(salad);
    }

    @Test
    void create_shouldFail_withZeroFoundationIngredients() throws DatabaseFailureException {
        // Arrange
        Salad salad = createTestSalad();
        salad.setFoodIngredients(List.of(
            createIngredient("food-1", false),  // not foundation
            createIngredient("food-2", false)   // not foundation
        ));

        when(foodService.findByExtidIn(anyList())).thenReturn(List.of(
            createFood("food-1", false),
            createFood("food-2", false)
        ));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            service.create(salad);
        });

        assertEquals("Salad must have at least one foundation ingredient", exception.getMessage());
        verify(dbService, never()).create(any(Salad.class));
    }

    @Test
    void create_shouldSucceed_withFourFoundationIngredients() throws DatabaseFailureException {
        // Arrange
        Salad salad = createTestSalad();
        salad.setFoodIngredients(List.of(
            createIngredient("food-1", true),   // 1 foundation
            createIngredient("food-2", true),   // 2 foundation
            createIngredient("food-3", true),   // 3 foundation
            createIngredient("food-4", true)    // 4 foundation - now allowed!
        ));

        when(foodService.findByExtidIn(anyList())).thenReturn(List.of(
            createFood("food-1", true),
            createFood("food-2", true),
            createFood("food-3", true),
            createFood("food-4", true)
        ));

        when(dbService.create(any(Salad.class))).thenReturn(salad);

        // Act
        Salad result = service.create(salad);

        // Assert
        assertNotNull(result);
        verify(dbService, times(1)).create(salad);
    }

    @Test
    void create_shouldFail_withNoIngredients() {
        // Arrange
        Salad salad = createTestSalad();
        salad.setFoodIngredients(new ArrayList<>());  // Empty list

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            service.create(salad);
        });

        assertEquals("Salad must have at least one ingredient", exception.getMessage());
        verify(dbService, never()).create(any(Salad.class));
    }

    @Test
    void create_shouldFail_withNullIngredients() {
        // Arrange
        Salad salad = createTestSalad();
        salad.setFoodIngredients(null);  // Null list

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            service.create(salad);
        });

        assertEquals("Salad must have at least one ingredient", exception.getMessage());
        verify(dbService, never()).create(any(Salad.class));
    }

    @Test
    void update_shouldSucceed_withValidFoundationCount() throws DatabaseFailureException {
        // Arrange
        String extid = "salad-123";
        Salad salad = createTestSalad();
        salad.setFoodIngredients(List.of(
            createIngredient("food-1", true),   // 1 foundation
            createIngredient("food-2", false)   // not foundation
        ));

        when(foodService.findByExtidIn(anyList())).thenReturn(List.of(
            createFood("food-1", true),
            createFood("food-2", false)
        ));
        when(dbService.update(eq(extid), any(Salad.class))).thenReturn(salad);

        // Act
        Salad result = service.update(extid, salad);

        // Assert
        assertNotNull(result);
        verify(dbService).update(extid, salad);
    }

    @Test
    void update_shouldFail_withInvalidFoundationCount() throws DatabaseFailureException {
        // Arrange
        String extid = "salad-123";
        Salad salad = createTestSalad();
        salad.setFoodIngredients(List.of(
            createIngredient("food-1", false),  // not foundation
            createIngredient("food-2", false)   // not foundation
        ));

        when(foodService.findByExtidIn(anyList())).thenReturn(List.of(
            createFood("food-1", false),
            createFood("food-2", false)
        ));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            service.update(extid, salad);
        });

        assertEquals("Salad must have at least one foundation ingredient", exception.getMessage());
        verify(dbService, never()).update(any(), any(Salad.class));
    }

    // Helper methods

    private Salad createTestSalad() {
        return Salad.builder()
            .name("Test Salad")
            .description("A test salad")
            .userExtid("user-123")
            .build();
    }

    private SaladFoodIngredient createIngredient(String foodExtid, boolean isFoundation) {
        return SaladFoodIngredient.builder()
            .foodExtid(foodExtid)
            .grams(100)
            .build();
    }

    private Food createFood(String extid, boolean isFoundation) {
        return Food.builder()
            .extid(extid)
            .name("Food " + extid)
            .foundation(isFoundation)
            .build();
    }
}
