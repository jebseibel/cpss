package com.seibel.cpss.database.db.repository;

import com.seibel.cpss.database.db.entity.PasswordResetTokenDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenDb, Long> {
    Optional<PasswordResetTokenDb> findByToken(String token);

    Optional<PasswordResetTokenDb> findByExtid(String extid);

    List<PasswordResetTokenDb> findByUserExtid(String userExtid);

    void deleteByUserExtid(String userExtid);

    boolean existsByToken(String token);
}
