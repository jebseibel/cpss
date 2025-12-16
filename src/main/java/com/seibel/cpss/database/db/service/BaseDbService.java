package com.seibel.cpss.database.db.service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseDbService {

    private final String entityName;
    protected final String domainName;

    protected BaseDbService(String entityName) {
        this.entityName = entityName;
        this.domainName = entityName.endsWith("Db")
                ? entityName.substring(0, entityName.length() - 2)
                : entityName;
    }

    // Success messages (for logging)
    protected String createdMessage(String extid) {
        return String.format("%s with extid=%s created successfully", domainName, extid);
    }

    protected String updatedMessage(String extid) {
        return String.format("%s with extid=%s updated successfully", domainName, extid);
    }

    protected String deletedMessage(String extid) {
        return String.format("%s with extid=%s deleted successfully", domainName, extid);
    }

    protected String foundByActiveMessage(String activeType, int count) {
        return String.format("Found %d %s record(s) with active=%s", count, domainName, activeType);
    }

    // Failure messages (for exceptions and error logs)
    protected String notFoundMessage(String extid) {
        return String.format("%s not found: %s", domainName, extid);
    }

    protected String failedOperationMessage(String operation) {
        return String.format("Failed to %s %s", operation, domainName);
    }

    protected String failedOperationMessage(String operation, String extid) {
        return String.format("Failed to %s %s: %s", operation, domainName, extid);
    }
}