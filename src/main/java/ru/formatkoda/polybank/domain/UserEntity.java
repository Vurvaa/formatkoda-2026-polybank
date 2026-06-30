package ru.formatkoda.polybank.domain;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public record UserEntity(
        long id,
        String login,
        String name,
        String lastName,
        String pswdHash,
        OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {
        public boolean isBlocked() {
                return blockedAt == null ? true : false;
        }
}
