package ru.formatkoda.polybank.domain;

import lombok.NonNull;

public record RoleEntity(
        @NonNull Long id,
        @NonNull String name
) {
}
