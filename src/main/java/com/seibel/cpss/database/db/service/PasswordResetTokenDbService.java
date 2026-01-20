package com.seibel.cpss.database.db.service;

import com.seibel.cpss.common.domain.PasswordResetToken;
import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.PasswordResetTokenDb;
import com.seibel.cpss.database.db.exceptions.DatabaseAccessException;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.mapper.PasswordResetTokenMapper;
import com.seibel.cpss.database.db.repository.PasswordResetTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PasswordResetTokenDbService extends BaseDbService {

    private final PasswordResetTokenRepository repository;
    private final PasswordResetTokenMapper mapper;

    public PasswordResetTokenDbService(PasswordResetTokenRepository repository, PasswordResetTokenMapper mapper) {
        super("PasswordResetTokenDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public PasswordResetToken create(PasswordResetToken item) {
        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            PasswordResetTokenDb entity = mapper.toDb(item);
            entity.setExtid(extid);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setActive(ActiveEnum.ACTIVE);
            PasswordResetTokenDb saved = repository.save(entity);
            log.info(createdMessage(extid));
            return mapper.toModel(saved);
        } catch (Exception e) {
            log.error(failedOperationMessage("create", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("create"), e);
        }
    }

    public PasswordResetToken findByToken(String token) {
        try {
            Optional<PasswordResetTokenDb> db = repository.findByToken(token);
            return db.map(mapper::toModel)
                    .orElseThrow(() -> new DatabaseAccessException(notFoundMessage(token)));
        } catch (Exception e) {
            log.error(failedOperationMessage("find", token), e);
            throw new DatabaseFailureException(failedOperationMessage("find"), e);
        }
    }

    public PasswordResetToken findByExtid(String extid) {
        try {
            Optional<PasswordResetTokenDb> db = repository.findByExtid(extid);
            return db.map(mapper::toModel)
                    .orElseThrow(() -> new DatabaseAccessException(notFoundMessage(extid)));
        } catch (Exception e) {
            log.error(failedOperationMessage("find", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("find"), e);
        }
    }

    public List<PasswordResetToken> findByUserExtid(String userExtid) {
        try {
            List<PasswordResetTokenDb> dbList = repository.findByUserExtid(userExtid);
            log.info(foundByActiveMessage("user=" + userExtid, dbList.size()));
            return mapper.toModelList(dbList);
        } catch (Exception e) {
            log.error(failedOperationMessage("find by user", userExtid), e);
            throw new DatabaseFailureException(failedOperationMessage("find by user"), e);
        }
    }

    public void deleteByUserExtid(String userExtid) {
        try {
            repository.deleteByUserExtid(userExtid);
            log.info("Deleted all password reset tokens for user: {}", userExtid);
        } catch (Exception e) {
            log.error(failedOperationMessage("delete by user", userExtid), e);
            throw new DatabaseFailureException(failedOperationMessage("delete by user"), e);
        }
    }

    public boolean existsByToken(String token) {
        try {
            return repository.existsByToken(token);
        } catch (Exception e) {
            log.error("Failed to check if token exists: {}", token, e);
            throw new DatabaseFailureException("Failed to check if token exists", e);
        }
    }

    public boolean delete(String extid) {
        LocalDateTime now = LocalDateTime.now();

        try {
            PasswordResetTokenDb entity = repository.findByExtid(extid)
                    .orElseThrow(() -> new DatabaseAccessException(notFoundMessage(extid)));
            entity.setActive(ActiveEnum.INACTIVE);
            entity.setDeletedAt(now);
            repository.save(entity);
            log.info(deletedMessage(extid));
            return true;
        } catch (Exception e) {
            log.error(failedOperationMessage("delete", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("delete"), e);
        }
    }

    public void markAsUsed(String token) {
        try {
            PasswordResetTokenDb entity = repository.findByToken(token)
                    .orElseThrow(() -> new DatabaseAccessException(notFoundMessage(token)));
            entity.setUsed(true);
            entity.setUpdatedAt(LocalDateTime.now());
            repository.save(entity);
            log.info("Password reset token marked as used: {}", token);
        } catch (Exception e) {
            log.error("Failed to mark token as used: {}", token, e);
            throw new DatabaseFailureException("Failed to mark token as used", e);
        }
    }
}
