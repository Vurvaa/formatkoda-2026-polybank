package ru.formatkoda.polybank.domain;

import lombok.NonNull;

public record UserLogin(
        @NonNull String login
) {
}
