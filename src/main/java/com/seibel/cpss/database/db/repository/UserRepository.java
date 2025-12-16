package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.database.db.entity.UserDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserDb, Long> {
    Optional<UserDb> findByUsername(String username);
    Optional<UserDb> findByExtid(String extid);
    boolean existsByUsername(String username);
    boolean existsByExtid(String extid);
}
