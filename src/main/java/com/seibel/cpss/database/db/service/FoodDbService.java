package com.seibel.cpss.database.db.service;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.common.util.CodeGenerator;
import com.seibel.cpss.database.db.entity.FoodDb;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.mapper.FoodMapper;
import com.seibel.cpss.database.db.mapper.NutritionMapper;
import com.seibel.cpss.database.db.repository.FoodRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FoodDbService extends BaseDbService {

    private final FoodRepository repository;
    private final FoodMapper mapper;
    private final NutritionMapper nutritionMapper;

    public FoodDbService(FoodRepository repository, FoodMapper mapper,
                         NutritionMapper nutritionMapper) {
        super("FoodDb");
        this.repository = repository;
        this.mapper = mapper;
        this.nutritionMapper = nutritionMapper;
    }

    public Food create(Food item) throws DatabaseFailureException {
        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            FoodDb entity = mapper.toDb(item);

            // Auto-generate code if not provided
            if (entity.getCode() == null || entity.getCode().trim().isEmpty()) {
                String generatedCode = CodeGenerator.generateCode(
                    entity.getName(),
                    entity.getCategory(),
                    entity.getSubcategory(),
                    code -> repository.findByCode(code).isPresent()
                );
                entity.setCode(generatedCode);
                log.info("Auto-generated code '{}' for food '{}'", generatedCode, entity.getName());
            }

            // Set foundation to false if not provided
            if (entity.getFoundation() == null) {
                entity.setFoundation(false);
            }

            entity.setExtid(extid);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setActive(ActiveEnum.ACTIVE);
            FoodDb saved = repository.save(entity);
            log.info(createdMessage(extid));
            return mapper.toModel(saved);
        } catch (Exception e) {
            log.error(failedOperationMessage("create", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("create"), e);
        }
    }

    public Food update(String extid, Food item) throws DatabaseFailureException {
        FoodDb existing = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(notFoundMessage(extid)));

        existing.setUpdatedAt(LocalDateTime.now());
        if (item.getName() != null) existing.setName(item.getName());
        if (item.getCategory() != null) existing.setCategory(item.getCategory());
        if (item.getSubcategory() != null) existing.setSubcategory(item.getSubcategory());
        if (item.getDescription() != null) existing.setDescription(item.getDescription());
        if (item.getNotes() != null) existing.setNotes(item.getNotes());
        if (item.getFoundation() != null) existing.setFoundation(item.getFoundation());

        FoodDb saved = repository.save(existing);
        log.info(updatedMessage(extid));
        return mapper.toModel(saved);
    }

    public boolean delete(String extid) throws DatabaseFailureException {
        FoodDb existing = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(notFoundMessage(extid)));

        existing.setDeletedAt(LocalDateTime.now());
        existing.setActive(ActiveEnum.INACTIVE);
        repository.save(existing);
        log.info(deletedMessage(extid));
        return true;
    }

    public Food findByExtid(String extid) throws DatabaseFailureException {
        return mapper.toModel(repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(notFoundMessage(extid))));
    }

    public List<Food> findAll() {
        List<Food> results = mapper.toModelList(repository.findAll());
        log.info(foundByActiveMessage("all", results.size()));
        return results;
    }

    public List<Food> findByActive(ActiveEnum active) {
        List<Food> results = mapper.toModelList(repository.findByActive(active));
        log.info(foundByActiveMessage(active.toString(), results.size()));
        return results;
    }

    public List<Food> findByExtidIn(List<String> extids) throws DatabaseFailureException {
        List<Food> results = mapper.toModelList(repository.findByExtidIn(extids));
        log.debug("Found {} foods by extid list", results.size());
        return results;
    }
}