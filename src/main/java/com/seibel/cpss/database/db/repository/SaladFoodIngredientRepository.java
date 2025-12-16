package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.database.db.entity.SaladFoodIngredientDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaladFoodIngredientRepository extends JpaRepository<SaladFoodIngredientDb, Long> {
    Optional<SaladFoodIngredientDb> findByExtid(String extid);
    List<SaladFoodIngredientDb> findBySaladId(Long saladId);
    boolean existsByExtid(String extid);
}
