package com.seibel.cpss.database.db.exceptions;

public class DatabaseFailureException extends RuntimeException {

    public DatabaseFailureException(String message) {
        super(message);
    }

    public DatabaseFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}