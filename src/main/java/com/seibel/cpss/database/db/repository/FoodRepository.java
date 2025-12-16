package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.FoodDb;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRepository extends ListCrudRepository<FoodDb, Long> {
    Optional<FoodDb> findByExtid(String extid);
    List<FoodDb> findByActive(ActiveEnum active);
    List<FoodDb> findByExtidIn(List<String> extids);
    boolean existsByExtid(String extid);
    Optional<FoodDb> findByCode(String code);
    Optional<FoodDb> findByName(String name);
}
