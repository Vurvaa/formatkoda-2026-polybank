package ru.formatkoda.polybank.domain.user;

import lombok.NonNull;

public record UserLogin(
        @NonNull String value
) {
}
