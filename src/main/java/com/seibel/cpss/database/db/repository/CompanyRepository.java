package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.CompanyDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyDb, Long> {
    Optional<CompanyDb> findByName(String name);
    Optional<CompanyDb> findByExtid(String extid);
    Optional<CompanyDb> findByCode(String code);
    Page<CompanyDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);
}
