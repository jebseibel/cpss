package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.NutritionDb;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NutritionRepository extends ListCrudRepository<NutritionDb, Long> {

    Optional<NutritionDb> findByExtid(String extid);
    List<NutritionDb> findByActive(ActiveEnum active);
    boolean existsByExtid(String extid);
    Optional<NutritionDb> findByCode(String code);
    Optional<NutritionDb> findByName(String name);
}

