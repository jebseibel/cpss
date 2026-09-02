package com.seibel.cpss.database.db.service;

import com.seibel.cpss.common.domain.FoodWarningAssessment;
import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.FoodWarningAssessmentDb;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.mapper.FoodWarningAssessmentMapper;
import com.seibel.cpss.database.db.repository.FoodWarningAssessmentRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FoodWarningAssessmentDbService extends BaseDbService {

    private final FoodWarningAssessmentRepository repository;
    private final FoodWarningAssessmentMapper mapper;

    public FoodWarningAssessmentDbService(FoodWarningAssessmentRepository repository,
                                          FoodWarningAssessmentMapper mapper) {
        super("FoodWarningAssessmentDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public FoodWarningAssessment create(@NonNull FoodWarningAssessment assessment) {
        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            assessment.setExtid(extid);
            assessment.setCreatedAt(now);
            assessment.setUpdatedAt(now);
            assessment.setActive(ActiveEnum.ACTIVE);
            if (assessment.getAssessedAt() == null) {
                assessment.setAssessedAt(now);
            }

            FoodWarningAssessmentDb saved = repository.save(mapper.toDb(assessment));

            log.info(createdMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            log.error(failedOperationMessage("create", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("create"), e);
        }
    }

    public FoodWarningAssessment findByExtid(@NonNull String extid) {
        FoodWarningAssessmentDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(notFoundMessage(extid)));
        return mapper.toModel(record);
    }

    public List<FoodWarningAssessment> findAll() {
        List<FoodWarningAssessmentDb> records = repository.findAllActive();
        log.info(foundByActiveMessage(ActiveEnum.ACTIVE.name(), records.size()));
        return mapper.toModelList(records);
    }

    public List<FoodWarningAssessment> findByFoodId(@NonNull Long foodId) {
        return mapper.toModelList(repository.findByFoodIdAndActive(foodId, ActiveEnum.ACTIVE));
    }

    public boolean hasBeenAssessed(@NonNull Long foodId) {
        return repository.existsByFoodIdAndActive(foodId, ActiveEnum.ACTIVE);
    }

    /** The agent's work queue: active foods with no assessment yet. */
    public List<Long> findUnassessedFoodIds() {
        return repository.findUnassessedFoodIds();
    }
}
