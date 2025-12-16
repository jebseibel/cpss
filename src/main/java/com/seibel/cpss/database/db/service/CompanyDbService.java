package com.seibel.cpss.database.db.service;

import com.seibel.cpss.common.domain.Company;
import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.common.util.CodeGenerator;
import com.seibel.cpss.database.db.entity.CompanyDb;
import com.seibel.cpss.database.db.exceptions.DatabaseFailureException;
import com.seibel.cpss.database.db.mapper.CompanyMapper;
import com.seibel.cpss.database.db.repository.CompanyRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CompanyDbService extends BaseDbService {

    private final CompanyRepository repository;
    private final CompanyMapper mapper;

    public CompanyDbService(CompanyRepository repository, CompanyMapper mapper) {
        super("CompanyDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Company create(String code, @NonNull String name, @NonNull String description) {
        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            CompanyDb record = new CompanyDb();
            record.setExtid(extid);

            // Auto-generate code if not provided
            if (code == null || code.trim().isEmpty()) {
                String generatedCode = CodeGenerator.generateCode(
                    name,
                    c -> repository.findByCode(c).isPresent()
                );
                record.setCode(generatedCode);
                log.info("Auto-generated code '{}' for company '{}'", generatedCode, name);
            } else {
                record.setCode(code);
            }

            record.setName(name);
            record.setDescription(description);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            CompanyDb saved = repository.save(record);
            log.info(createdMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            log.error(failedOperationMessage("create", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("create"), e);
        }
    }

    public Company update(@NonNull String extid, String code, String name, String description) {
        CompanyDb record = repository.findByExtid(extid).orElse(null);
        if (record == null) {
            log.warn(notFoundMessage(extid));
            return null;
        }
        try {
            record.setCode(code);
            record.setName(name);
            record.setDescription(description);
            record.setUpdatedAt(LocalDateTime.now());

            CompanyDb saved = repository.save(record);
            log.info(updatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            log.error(failedOperationMessage("update", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("update"), e);
        }
    }

    public boolean delete(@NonNull String extid) {
        CompanyDb record = repository.findByExtid(extid).orElse(null);
        if (record == null) {
            log.warn(notFoundMessage(extid));
            return false;
        }
        try {
            record.setDeletedAt(LocalDateTime.now());
            record.setActive(ActiveEnum.INACTIVE);

            repository.save(record);
            log.info(deletedMessage(extid));
            return true;

        } catch (Exception e) {
            log.error(failedOperationMessage("delete", extid), e);
            throw new DatabaseFailureException(failedOperationMessage("delete"), e);
        }
    }

    public Company findByExtid(@NonNull String extid) {
        CompanyDb record = repository.findByExtid(extid).orElse(null);
        if (record == null) {
            log.warn(notFoundMessage(extid));
            return null;
        }
        return mapper.toModel(record);
    }

    public List<Company> findAll() {
        return findAndLog(repository.findAll(), "all");
    }

    public Page<Company> findAll(Pageable pageable) {
        Page<CompanyDb> page = repository.findAll(pageable);
        log.info(foundByActiveMessage("all (pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Company> findByActive(@NonNull ActiveEnum activeEnum) {
        // Use paged method with unpaged to keep backward compatibility
        Page<CompanyDb> page = repository.findByActive(activeEnum, Pageable.unpaged());
        log.info(foundByActiveMessage(activeEnum.toString(), (int) page.getTotalElements()));
        return mapper.toModelList(page.getContent());
    }

    public Page<Company> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<CompanyDb> page = repository.findByActive(activeEnum, pageable);
        log.info(foundByActiveMessage(activeEnum.toString() + " (pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Company> findAndLog(List<CompanyDb> records, String type) {
        log.info(foundByActiveMessage(type, records.size()));
        return mapper.toModelList(records);
    }
}