package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.SaladDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaladRepository extends JpaRepository<SaladDb, Long> {

    @Query("SELECT DISTINCT s FROM SaladDb s " +
           "LEFT JOIN FETCH s.foodIngredients i " +
           "LEFT JOIN FETCH i.food f " +
           "LEFT JOIN FETCH f.nutrition " +
           "WHERE s.extid = :extid")
    Optional<SaladDb> findByExtid(@Param("extid") String extid);

    List<SaladDb> findByUserExtid(String userExtid);
    List<SaladDb> findByUserExtidAndActive(String userExtid, ActiveEnum active);

    @Query("SELECT DISTINCT s FROM SaladDb s " +
           "LEFT JOIN FETCH s.foodIngredients i " +
           "LEFT JOIN FETCH i.food f " +
           "LEFT JOIN FETCH f.nutrition")
    List<SaladDb> findAllWithIngredients();

    boolean existsByExtid(String extid);
}
