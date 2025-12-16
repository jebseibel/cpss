package com.seibel.cpss.database.db.service;

import com.seibel.cpss.common.domain.MixtureIngredient;
import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.MixtureIngredientDb;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.mapper.MixtureIngredientMapper;
import com.seibel.cpss.database.db.repository.MixtureIngredientRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class MixtureIngredientDbService extends BaseDbService {

    private final MixtureIngredientRepository repository;
    private final MixtureIngredientMapper mapper;

    public MixtureIngredientDbService(MixtureIngredientRepository repository, MixtureIngredientMapper mapper) {
        super("MixtureIngredientDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public MixtureIngredient create(@NonNull MixtureIngredient ingredient) {
        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            ingredient.setExtid(extid);
            ingredient.setCreatedAt(now);
            ingredient.setUpdatedAt(now);
            ingredient.setActive(ActiveEnum.ACTIVE);

            MixtureIngredientDb ingredientDb = mapper.toDb(ingredient);
            MixtureIngredientDb saved = repository.save(ingredientDb);

            log.info(createdMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            log.error(failedOperationMessage("create", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("create"), e);
        }
    }

    @Transactional
    public void deleteByMixtureId(@NonNull Long mixtureId) {
        try {
            List<MixtureIngredientDb> ingredients = repository.findByMixtureId(mixtureId);
            for (MixtureIngredientDb ingredient : ingredients) {
                ingredient.setDeletedAt(LocalDateTime.now());
                ingredient.setActive(ActiveEnum.INACTIVE);
                repository.save(ingredient);
            }
            log.info("Deleted ingredients for mixture ID: {}", mixtureId);

        } catch (Exception e) {
            log.error("Failed to delete ingredients for mixture ID: {}", mixtureId, e);
            throw new DatabaseFailureException("Failed to delete ingredients", e);
        }
    }

    public List<MixtureIngredient> findByMixtureId(@NonNull Long mixtureId) {
        try {
            List<MixtureIngredientDb> ingredients = repository.findByMixtureId(mixtureId);
            return mapper.toModelList(ingredients);

        } catch (Exception e) {
            log.error("Failed to find ingredients for mixture ID: {}", mixtureId, e);
            throw new DatabaseFailureException("Failed to find ingredients", e);
        }
    }
}
