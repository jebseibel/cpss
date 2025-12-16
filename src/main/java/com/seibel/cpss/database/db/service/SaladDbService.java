package com.seibel.cpss.database.db.service;

import com.seibel.cpss.common.domain.Salad;
import com.seibel.cpss.common.domain.SaladFoodIngredient;
import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.SaladDb;
import com.seibel.cpss.database.db.entity.SaladFoodIngredientDb;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.mapper.SaladFoodIngredientMapper;
import com.seibel.cpss.database.db.mapper.SaladMapper;
import com.seibel.cpss.database.db.repository.SaladRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SaladDbService extends BaseDbService {

    private final SaladRepository repository;
    private final SaladMapper mapper;
    private final SaladFoodIngredientMapper ingredientMapper;

    public SaladDbService(SaladRepository repository, SaladMapper mapper, SaladFoodIngredientMapper ingredientMapper) {
        super("SaladDb");
        this.repository = repository;
        this.mapper = mapper;
        this.ingredientMapper = ingredientMapper;
    }

    @Transactional
    public Salad create(@NonNull Salad salad) {
        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            salad.setExtid(extid);
            salad.setCreatedAt(now);
            salad.setUpdatedAt(now);
            salad.setActive(ActiveEnum.ACTIVE);

            SaladDb saladDb = mapper.toDb(salad);

            // Handle food ingredients
            if (salad.getFoodIngredients() != null && !salad.getFoodIngredients().isEmpty()) {
                List<SaladFoodIngredientDb> ingredientDbs = new ArrayList<>();
                for (SaladFoodIngredient ingredient : salad.getFoodIngredients()) {
                    ingredient.setExtid(UUID.randomUUID().toString());
                    ingredient.setCreatedAt(now);
                    ingredient.setUpdatedAt(now);
                    ingredient.setActive(ActiveEnum.ACTIVE);
                    ingredient.setSaladId(null); // Will be set by cascade

                    SaladFoodIngredientDb ingredientDb = ingredientMapper.toDb(ingredient);
                    ingredientDb.setSalad(saladDb);
                    ingredientDbs.add(ingredientDb);
                }
                saladDb.setFoodIngredients(ingredientDbs);
            }

            SaladDb saved = repository.save(saladDb);

            log.info(createdMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            log.error(failedOperationMessage("create", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("create"), e);
        }
    }

    @Transactional
    public Salad update(@NonNull String extid, @NonNull Salad salad) {
        SaladDb record = repository.findByExtid(extid).orElse(null);
        if (record == null) {
            log.warn(notFoundMessage(extid));
            return null;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            record.setName(salad.getName());
            record.setDescription(salad.getDescription());
            record.setUpdatedAt(now);

            // Handle food ingredients update - clear and replace
            if (salad.getFoodIngredients() != null) {
                record.getFoodIngredients().clear();

                List<SaladFoodIngredientDb> ingredientDbs = new ArrayList<>();
                for (SaladFoodIngredient ingredient : salad.getFoodIngredients()) {
                    ingredient.setExtid(UUID.randomUUID().toString());
                    ingredient.setCreatedAt(now);
                    ingredient.setUpdatedAt(now);
                    ingredient.setActive(ActiveEnum.ACTIVE);

                    SaladFoodIngredientDb ingredientDb = ingredientMapper.toDb(ingredient);
                    ingredientDb.setSalad(record);
                    ingredientDbs.add(ingredientDb);
                }
                record.getFoodIngredients().addAll(ingredientDbs);
            }

            SaladDb updated = repository.save(record);
            log.info(updatedMessage(extid));
            return mapper.toModel(updated);

        } catch (Exception e) {
            log.error(failedOperationMessage("update", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("update"), e);
        }
    }

    @Transactional
    public void delete(@NonNull String extid) {
        SaladDb record = repository.findByExtid(extid).orElse(null);
        if (record == null) {
            log.warn(notFoundMessage(extid));
            return;
        }

        try {
            record.setDeletedAt(LocalDateTime.now());
            record.setActive(ActiveEnum.INACTIVE);
            repository.save(record);
            log.info(deletedMessage(extid));

        } catch (Exception e) {
            log.error(failedOperationMessage("delete", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("delete"), e);
        }
    }

    public Salad findByExtid(@NonNull String extid) {
        try {
            return repository.findByExtid(extid)
                    .map(mapper::toModel)
                    .orElse(null);

        } catch (Exception e) {
            log.error(failedOperationMessage("findByExtid", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("findByExtid"), e);
        }
    }

    public List<Salad> findByUserExtid(@NonNull String userExtid) {
        try {
            List<SaladDb> salads = repository.findByUserExtidAndActive(userExtid, ActiveEnum.ACTIVE);
            return mapper.toModelList(salads);

        } catch (Exception e) {
            log.error("Failed to find salads for user: {}", userExtid, e);
            throw new DatabaseFailureException("Failed to find salads for user", e);
        }
    }

    public List<Salad> findAll() {
        try {
            List<SaladDb> all = repository.findAllWithIngredients();
            return mapper.toModelList(all);

        } catch (Exception e) {
            log.error("Failed to retrieve all salads", e);
            throw new DatabaseFailureException("Failed to retrieve all salads", e);
        }
    }
}
