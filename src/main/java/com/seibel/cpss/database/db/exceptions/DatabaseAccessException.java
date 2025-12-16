package com.seibel.cpss.database.db.exceptions;

public class DatabaseAccessException extends DatabaseFailureException {

    public DatabaseAccessException(String message) {
        super(message);
    }

    public DatabaseAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}