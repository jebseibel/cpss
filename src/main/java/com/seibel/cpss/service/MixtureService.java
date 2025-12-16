package com.seibel.cpss.service;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.Mixture;
import com.seibel.cpss.common.domain.MixtureIngredient;
import com.seibel.cpss.common.exceptions.ResourceNotFoundException;
import com.seibel.cpss.common.exceptions.ServiceException;
import com.seibel.cpss.common.exceptions.ValidationException;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.service.MixtureDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class MixtureService extends BaseService {

    private final MixtureDbService dbService;
    private final FoodService foodService;

    public MixtureService(MixtureDbService dbService, FoodService foodService) {
        super(Mixture.class.getSimpleName());
        this.dbService = dbService;
        this.foodService = foodService;
    }

    @Transactional
    public Mixture create(Mixture mixture) {
        requireNonNull(mixture, "Mixture");
        requireNonBlank(mixture.getName(), "name");
        requireNonBlank(mixture.getUserExtid(), "userExtid");
        log.info("create(): {}", mixture.getName());

        // Validate ingredients
        validateIngredients(mixture);

        try {
            return dbService.create(mixture);
        } catch (DatabaseFailureException e) {
            log.error("Failed to create mixture: {}", mixture.getName(), e);
            throw new ServiceException("Unable to create mixture", e);
        }
    }

    @Transactional
    public Mixture update(String extid, Mixture mixture) {
        requireNonBlank(extid, "extid");
        requireNonNull(mixture, "Mixture");
        log.info("update(): extid={}, {}", extid, mixture.getName());

        // Validate ingredients
        validateIngredients(mixture);

        try {
            Mixture updated = dbService.update(extid, mixture);
            if (updated == null) {
                throw new ResourceNotFoundException("Mixture", extid);
            }
            return updated;
        } catch (DatabaseFailureException e) {
            log.error("Failed to update mixture: {}", extid, e);
            throw new ServiceException("Unable to update mixture", e);
        }
    }

    @Transactional
    public void delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            dbService.delete(extid);
        } catch (DatabaseFailureException e) {
            log.error("Failed to delete mixture: {}", extid, e);
            throw new ServiceException("Unable to delete mixture", e);
        }
    }

    public Mixture findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.debug("findByExtid(): extid={}", extid);

        try {
            Mixture mixture = dbService.findByExtid(extid);
            if (mixture == null) {
                throw new ResourceNotFoundException("Mixture", extid);
            }
            return mixture;
        } catch (DatabaseFailureException e) {
            log.error("Failed to find mixture: {}", extid, e);
            throw new ServiceException("Unable to find mixture", e);
        }
    }

    public List<Mixture> findByUserExtid(String userExtid) {
        requireNonBlank(userExtid, "userExtid");
        log.debug("findByUserExtid(): userExtid={}", userExtid);

        try {
            return dbService.findByUserExtid(userExtid);
        } catch (DatabaseFailureException e) {
            log.error("Failed to find mixtures for user: {}", userExtid, e);
            throw new ServiceException("Unable to find mixtures for user", e);
        }
    }

    public List<Mixture> findAll() {
        log.debug("findAll()");

        try {
            return dbService.findAll();
        } catch (DatabaseFailureException e) {
            log.error("Failed to retrieve all mixtures", e);
            throw new ServiceException("Unable to retrieve mixtures", e);
        }
    }

    private void validateIngredients(Mixture mixture) {
        if (mixture.getIngredients() == null || mixture.getIngredients().isEmpty()) {
            throw new ValidationException("Mixture must have at least one ingredient");
        }

        for (MixtureIngredient ingredient : mixture.getIngredients()) {
            if (ingredient.getFoodExtid() == null || ingredient.getFoodExtid().isBlank()) {
                throw new ValidationException("Ingredient must have a food extid");
            }

            // Validate grams
            if (ingredient.getGrams() == null || ingredient.getGrams() <= 0) {
                throw new ValidationException("Ingredient grams must be greater than 0");
            }

            // Verify the food exists and is mixable
            Food food = foodService.findByExtid(ingredient.getFoodExtid());
            if (food == null) {
                throw new ValidationException("Food not found: " + ingredient.getFoodExtid());
            }

            if (!food.getMixable()) {
                throw new ValidationException("Food is not mixable: " + food.getName());
            }
        }
    }
}
