package com.seibel.cpss.service;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.Salad;
import com.seibel.cpss.common.domain.SaladFoodIngredient;
import com.seibel.cpss.common.exceptions.ResourceNotFoundException;
import com.seibel.cpss.common.exceptions.ServiceException;
import com.seibel.cpss.common.exceptions.ValidationException;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.service.SaladDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class SaladService extends BaseService {

    private final SaladDbService dbService;
    private final FoodService foodService;

    public SaladService(SaladDbService dbService, FoodService foodService) {
        super(Salad.class.getSimpleName());
        this.dbService = dbService;
        this.foodService = foodService;
    }

    @Transactional
    public Salad create(Salad salad) {
        requireNonNull(salad, "Salad");
        requireNonBlank(salad.getName(), "name");
        requireNonBlank(salad.getUserExtid(), "userExtid");
        validateFoundationCount(salad);
        log.info("create(): {}", salad.getName());

        try {
            return dbService.create(salad);
        } catch (DatabaseFailureException e) {
            log.error("Failed to create salad: {}", salad.getName(), e);
            throw new ServiceException("Unable to create salad", e);
        }
    }

    @Transactional
    public Salad update(String extid, Salad salad) {
        requireNonBlank(extid, "extid");
        requireNonNull(salad, "Salad");
        requireNonBlank(salad.getName(), "name");
        validateFoundationCount(salad);
        log.info("update(): extid={}, {}", extid, salad.getName());

        try {
            Salad updated = dbService.update(extid, salad);
            if (updated == null) {
                throw new ResourceNotFoundException("Salad", extid);
            }
            return updated;
        } catch (DatabaseFailureException e) {
            log.error("Failed to update salad: {}", extid, e);
            throw new ServiceException("Unable to update salad", e);
        }
    }

    @Transactional
    public void delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            dbService.delete(extid);
        } catch (DatabaseFailureException e) {
            log.error("Failed to delete salad: {}", extid, e);
            throw new ServiceException("Unable to delete salad", e);
        }
    }

    public Salad findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.debug("findByExtid(): extid={}", extid);

        try {
            Salad salad = dbService.findByExtid(extid);
            if (salad == null) {
                throw new ResourceNotFoundException("Salad", extid);
            }
            return salad;
        } catch (DatabaseFailureException e) {
            log.error("Failed to find salad: {}", extid, e);
            throw new ServiceException("Unable to find salad", e);
        }
    }

    public List<Salad> findByUserExtid(String userExtid) {
        requireNonBlank(userExtid, "userExtid");
        log.debug("findByUserExtid(): userExtid={}", userExtid);

        try {
            return dbService.findByUserExtid(userExtid);
        } catch (DatabaseFailureException e) {
            log.error("Failed to find salads for user: {}", userExtid, e);
            throw new ServiceException("Unable to find salads for user", e);
        }
    }

    public List<Salad> findAll() {
        log.debug("findAll()");

        try {
            return dbService.findAll();
        } catch (DatabaseFailureException e) {
            log.error("Failed to retrieve all salads", e);
            throw new ServiceException("Unable to retrieve salads", e);
        }
    }

    /**
     * Validates that a salad has at least 1 foundation ingredient.
     * Foundation ingredients are those with foundation=true.
     *
     * @param salad the salad to validate
     * @throws ValidationException if the foundation count is less than 1
     */
    private void validateFoundationCount(Salad salad) {
        if (salad.getFoodIngredients() == null || salad.getFoodIngredients().isEmpty()) {
            throw new ValidationException("Salad must have at least one ingredient");
        }

        // Collect all food extids that need to be fetched
        List<String> foodExtids = salad.getFoodIngredients().stream()
                .map(SaladFoodIngredient::getFoodExtid)
                .distinct()
                .toList();

        // Fetch all foods in a single query to avoid N+1
        List<Food> foods;
        try {
            foods = foodService.findByExtidIn(foodExtids);
        } catch (DatabaseFailureException e) {
            log.error("Failed to lookup foods for foundation validation", e);
            throw new ServiceException("Unable to validate salad ingredients", e);
        }

        // Count foundation ingredients
        long foundationCount = foods.stream()
                .filter(food -> Boolean.TRUE.equals(food.getFoundation()))
                .count();

        if (foundationCount < 1) {
            throw new ValidationException("Salad must have at least one foundation ingredient");
        }

        log.debug("Salad has {} foundation ingredients - validation passed", foundationCount);
    }
}
