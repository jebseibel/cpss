package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.database.db.entity.MixtureIngredientDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MixtureIngredientRepository extends JpaRepository<MixtureIngredientDb, Long> {
    Optional<MixtureIngredientDb> findByExtid(String extid);
    List<MixtureIngredientDb> findByMixtureId(Long mixtureId);
    boolean existsByExtid(String extid);
}
