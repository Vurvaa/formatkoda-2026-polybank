package ru.formatkoda.polybank.security;

import java.util.Objects;

public record UserSession(
        String login
) {
    public UserSession {
        Objects.requireNonNull(login);
    }
}