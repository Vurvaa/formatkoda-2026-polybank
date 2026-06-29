package ru.formatkoda.polybank.exceptions;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
