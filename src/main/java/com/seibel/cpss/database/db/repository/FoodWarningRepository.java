package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.common.enums.WarningStatusEnum;
import com.seibel.cpss.database.db.entity.FoodWarningDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodWarningRepository extends JpaRepository<FoodWarningDb, Long> {

    Optional<FoodWarningDb> findByExtid(String extid);

    List<FoodWarningDb> findByActive(ActiveEnum active);

    Page<FoodWarningDb> findByActive(ActiveEnum active, Pageable pageable);

    boolean existsByExtid(String extid);

    /** The serving path: only approved warnings are ever exposed to users. */
    List<FoodWarningDb> findByFoodIdAndStatusAndActive(Long foodId, WarningStatusEnum status, ActiveEnum active);

    /** Bulk serving path for the salad builder, which needs many foods at once. */
    List<FoodWarningDb> findByFoodIdInAndStatusAndActive(List<Long> foodIds, WarningStatusEnum status, ActiveEnum active);

    /** The review queue. */
    List<FoodWarningDb> findByStatusAndActive(WarningStatusEnum status, ActiveEnum active);

    Page<FoodWarningDb> findByStatusAndActive(WarningStatusEnum status, ActiveEnum active, Pageable pageable);

    default List<FoodWarningDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
