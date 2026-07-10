package ru.formatkoda.polybank.domain.user;

import lombok.NonNull;

public record RoleEntity(
        Long id,
        @NonNull String name
) {
}
