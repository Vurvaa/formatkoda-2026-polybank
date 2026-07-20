package ru.formatkoda.authorization.auth.domain;

import lombok.NonNull;

public record UserLogin(
        @NonNull String value
) {
}
