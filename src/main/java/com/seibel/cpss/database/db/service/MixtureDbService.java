package com.seibel.cpss.database.db.service;

import com.seibel.cpss.common.domain.Mixture;
import com.seibel.cpss.common.domain.MixtureIngredient;
import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.MixtureDb;
import com.seibel.cpss.database.db.entity.MixtureIngredientDb;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.mapper.MixtureIngredientMapper;
import com.seibel.cpss.database.db.mapper.MixtureMapper;
import com.seibel.cpss.database.db.repository.MixtureRepository;
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
public class MixtureDbService extends BaseDbService {

    private final MixtureRepository repository;
    private final MixtureMapper mapper;
    private final MixtureIngredientMapper ingredientMapper;

    public MixtureDbService(MixtureRepository repository, MixtureMapper mapper, MixtureIngredientMapper ingredientMapper) {
        super("MixtureDb");
        this.repository = repository;
        this.mapper = mapper;
        this.ingredientMapper = ingredientMapper;
    }

    @Transactional
    public Mixture create(@NonNull Mixture mixture) {
        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            mixture.setExtid(extid);
            mixture.setCreatedAt(now);
            mixture.setUpdatedAt(now);
            mixture.setActive(ActiveEnum.ACTIVE);

            MixtureDb mixtureDb = mapper.toDb(mixture);

            // Handle ingredients
            if (mixture.getIngredients() != null && !mixture.getIngredients().isEmpty()) {
                List<MixtureIngredientDb> ingredientDbs = new ArrayList<>();
                for (MixtureIngredient ingredient : mixture.getIngredients()) {
                    ingredient.setExtid(UUID.randomUUID().toString());
                    ingredient.setCreatedAt(now);
                    ingredient.setUpdatedAt(now);
                    ingredient.setActive(ActiveEnum.ACTIVE);
                    ingredient.setMixtureId(null); // Will be set by cascade

                    MixtureIngredientDb ingredientDb = ingredientMapper.toDb(ingredient);
                    ingredientDb.setMixture(mixtureDb);
                    ingredientDbs.add(ingredientDb);
                }
                mixtureDb.setIngredients(ingredientDbs);
            }

            MixtureDb saved = repository.save(mixtureDb);

            log.info(createdMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            log.error(failedOperationMessage("create", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("create"), e);
        }
    }

    @Transactional
    public Mixture update(@NonNull String extid, @NonNull Mixture mixture) {
        MixtureDb record = repository.findByExtid(extid).orElse(null);
        if (record == null) {
            log.warn(notFoundMessage(extid));
            return null;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            record.setName(mixture.getName());
            record.setDescription(mixture.getDescription());
            record.setUpdatedAt(now);

            // Handle ingredients update - clear and replace
            if (mixture.getIngredients() != null) {
                record.getIngredients().clear();

                List<MixtureIngredientDb> ingredientDbs = new ArrayList<>();
                for (MixtureIngredient ingredient : mixture.getIngredients()) {
                    ingredient.setExtid(UUID.randomUUID().toString());
                    ingredient.setCreatedAt(now);
                    ingredient.setUpdatedAt(now);
                    ingredient.setActive(ActiveEnum.ACTIVE);

                    MixtureIngredientDb ingredientDb = ingredientMapper.toDb(ingredient);
                    ingredientDb.setMixture(record);
                    ingredientDbs.add(ingredientDb);
                }
                record.getIngredients().addAll(ingredientDbs);
            }

            MixtureDb updated = repository.save(record);
            log.info(updatedMessage(extid));
            return mapper.toModel(updated);

        } catch (Exception e) {
            log.error(failedOperationMessage("update", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("update"), e);
        }
    }

    @Transactional
    public void delete(@NonNull String extid) {
        MixtureDb record = repository.findByExtid(extid).orElse(null);
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

    public Mixture findByExtid(@NonNull String extid) {
        try {
            return repository.findByExtid(extid)
                    .map(mapper::toModel)
                    .orElse(null);

        } catch (Exception e) {
            log.error(failedOperationMessage("findByExtid", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("findByExtid"), e);
        }
    }

    public List<Mixture> findByUserExtid(@NonNull String userExtid) {
        try {
            List<MixtureDb> mixtures = repository.findByUserExtidAndActive(userExtid, ActiveEnum.ACTIVE);
            return mapper.toModelList(mixtures);

        } catch (Exception e) {
            log.error("Failed to find mixtures for user: {}", userExtid, e);
            throw new DatabaseFailureException("Failed to find mixtures for user", e);
        }
    }

    public List<Mixture> findAll() {
        try {
            List<MixtureDb> all = repository.findAllWithIngredients();
            return mapper.toModelList(all);

        } catch (Exception e) {
            log.error("Failed to retrieve all mixtures", e);
            throw new DatabaseFailureException("Failed to retrieve all mixtures", e);
        }
    }
}
