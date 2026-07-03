package ru.formatkoda.polybank.security;

import lombok.NonNull;

public record UserSession(
        @NonNull String login
) {
}