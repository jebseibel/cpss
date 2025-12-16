package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.MixtureDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MixtureRepository extends JpaRepository<MixtureDb, Long> {

    @Query("SELECT DISTINCT m FROM MixtureDb m " +
           "LEFT JOIN FETCH m.ingredients i " +
           "LEFT JOIN FETCH i.food f " +
           "LEFT JOIN FETCH f.nutrition " +
           "WHERE m.extid = :extid")
    Optional<MixtureDb> findByExtid(@Param("extid") String extid);

    Optional<MixtureDb> findByName(String name);

    List<MixtureDb> findByUserExtid(String userExtid);

    List<MixtureDb> findByUserExtidAndActive(String userExtid, ActiveEnum active);

    @Query("SELECT DISTINCT m FROM MixtureDb m " +
           "LEFT JOIN FETCH m.ingredients i " +
           "LEFT JOIN FETCH i.food f " +
           "LEFT JOIN FETCH f.nutrition")
    List<MixtureDb> findAllWithIngredients();

    boolean existsByExtid(String extid);
}
