package com.seibel.cpss.database.db.service;

import com.seibel.cpss.common.domain.FoodWarning;
import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.common.enums.WarningStatusEnum;
import com.seibel.cpss.database.db.entity.FoodWarningDb;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.mapper.FoodWarningMapper;
import com.seibel.cpss.database.db.repository.FoodWarningRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FoodWarningDbService extends BaseDbService {

    private final FoodWarningRepository repository;
    private final FoodWarningMapper mapper;

    public FoodWarningDbService(FoodWarningRepository repository, FoodWarningMapper mapper) {
        super("FoodWarningDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Warnings are always created PENDING regardless of what the caller passes.
     * The agent has no path to publish directly — approval is a separate,
     * human-driven transition.
     */
    @Transactional
    public FoodWarning create(@NonNull FoodWarning warning) {
        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            warning.setExtid(extid);
            warning.setCreatedAt(now);
            warning.setUpdatedAt(now);
            warning.setActive(ActiveEnum.ACTIVE);
            warning.setStatus(WarningStatusEnum.PENDING);

            FoodWarningDb saved = repository.save(mapper.toDb(warning));

            log.info(createdMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            log.error(failedOperationMessage("create", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("create"), e);
        }
    }

    @Transactional
    public FoodWarning update(@NonNull String extid, @NonNull FoodWarning warning) {
        try {
            FoodWarningDb record = repository.findByExtid(extid)
                    .orElseThrow(() -> new DatabaseFailureException(notFoundMessage(extid)));

            if (warning.getCategory() != null) record.setCategory(warning.getCategory());
            if (warning.getSeverity() != null) record.setSeverity(warning.getSeverity());
            if (warning.getAppliesTo() != null) record.setAppliesTo(warning.getAppliesTo());
            if (warning.getWarningText() != null) record.setWarningText(warning.getWarningText());
            if (warning.getSources() != null) record.setSources(warning.getSources());
            if (warning.getConfidence() != null) record.setConfidence(warning.getConfidence());
            if (warning.getStatus() != null) record.setStatus(warning.getStatus());
            if (warning.getReviewedBy() != null) record.setReviewedBy(warning.getReviewedBy());
            if (warning.getReviewedAt() != null) record.setReviewedAt(warning.getReviewedAt());

            record.setUpdatedAt(LocalDateTime.now());

            FoodWarningDb saved = repository.save(record);
            log.info(updatedMessage(extid));
            return mapper.toModel(saved);

        } catch (DatabaseFailureException e) {
            throw e;
        } catch (Exception e) {
            log.error(failedOperationMessage("update", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("update"), e);
        }
    }

    /** Records a human review decision — the only path to APPROVED. */
    @Transactional
    public FoodWarning review(@NonNull String extid, @NonNull WarningStatusEnum decision, String reviewerExtid) {
        FoodWarning update = new FoodWarning();
        update.setStatus(decision);
        update.setReviewedBy(reviewerExtid);
        update.setReviewedAt(LocalDateTime.now());
        return update(extid, update);
    }

    @Transactional
    public boolean delete(@NonNull String extid) {
        try {
            FoodWarningDb record = repository.findByExtid(extid)
                    .orElseThrow(() -> new DatabaseFailureException(notFoundMessage(extid)));

            record.setDeletedAt(LocalDateTime.now());
            record.setActive(ActiveEnum.INACTIVE);
            repository.save(record);

            log.info(deletedMessage(extid));
            return true;

        } catch (DatabaseFailureException e) {
            throw e;
        } catch (Exception e) {
            log.error(failedOperationMessage("delete", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("delete"), e);
        }
    }

    public FoodWarning findByExtid(@NonNull String extid) {
        FoodWarningDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(notFoundMessage(extid)));
        return mapper.toModel(record);
    }

    public List<FoodWarning> findAll() {
        return findAndLog(repository.findAllActive(), ActiveEnum.ACTIVE.name());
    }

    public Page<FoodWarning> findAll(Pageable pageable) {
        return repository.findByActive(ActiveEnum.ACTIVE, pageable).map(mapper::toModel);
    }

    public List<FoodWarning> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum), activeEnum.name());
    }

    /** Approved warnings for one food — what the API serves. */
    public List<FoodWarning> findApprovedByFoodId(@NonNull Long foodId) {
        return mapper.toModelList(
                repository.findByFoodIdAndStatusAndActive(foodId, WarningStatusEnum.APPROVED, ActiveEnum.ACTIVE));
    }

    /** Approved warnings for many foods — the salad builder's aggregation input. */
    public List<FoodWarning> findApprovedByFoodIds(@NonNull List<Long> foodIds) {
        if (foodIds.isEmpty()) {
            return List.of();
        }
        return mapper.toModelList(
                repository.findByFoodIdInAndStatusAndActive(foodIds, WarningStatusEnum.APPROVED, ActiveEnum.ACTIVE));
    }

    /** The review queue. */
    public List<FoodWarning> findByStatus(@NonNull WarningStatusEnum status) {
        return mapper.toModelList(repository.findByStatusAndActive(status, ActiveEnum.ACTIVE));
    }

    private List<FoodWarning> findAndLog(List<FoodWarningDb> records, String type) {
        log.info(foundByActiveMessage(type, records.size()));
        return mapper.toModelList(records);
    }
}
