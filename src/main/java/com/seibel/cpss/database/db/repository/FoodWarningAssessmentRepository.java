package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.FoodWarningAssessmentDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodWarningAssessmentRepository extends JpaRepository<FoodWarningAssessmentDb, Long> {

    Optional<FoodWarningAssessmentDb> findByExtid(String extid);

    List<FoodWarningAssessmentDb> findByActive(ActiveEnum active);

    boolean existsByExtid(String extid);

    List<FoodWarningAssessmentDb> findByFoodIdAndActive(Long foodId, ActiveEnum active);

    boolean existsByFoodIdAndActive(Long foodId, ActiveEnum active);

    /**
     * The work queue: foods the agent has never assessed. Drives both the
     * initial sweep and re-runs after new foods are added.
     */
    @Query("SELECT f.id FROM FoodDb f WHERE f.active = com.seibel.cpss.common.enums.ActiveEnum.ACTIVE " +
           "AND f.id NOT IN (SELECT a.foodId FROM FoodWarningAssessmentDb a " +
           "WHERE a.active = com.seibel.cpss.common.enums.ActiveEnum.ACTIVE)")
    List<Long> findUnassessedFoodIds();

    default List<FoodWarningAssessmentDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
