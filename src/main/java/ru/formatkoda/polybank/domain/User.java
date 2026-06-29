package ru.formatkoda.polybank.domain;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public record User(
        long id,
        @NotBlank
        String login,
        String name,
        String lastName,
        @NotBlank
        String pswdHash,
        OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {
}
