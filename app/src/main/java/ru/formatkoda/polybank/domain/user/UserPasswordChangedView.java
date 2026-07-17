package ru.formatkoda.polybank.domain.user;

import lombok.NonNull;

public record UserPasswordChangedView(
        @NonNull UserLogin login,
        @NonNull String oldPassword,
        @NonNull String newPasswordHash
) {
}
