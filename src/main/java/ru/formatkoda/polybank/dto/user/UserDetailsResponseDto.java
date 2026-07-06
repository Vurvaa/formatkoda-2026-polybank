package ru.formatkoda.polybank.dto.user;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record UserDetailsResponseDto(
        Long id,
        @NotNull String login,
        @NotNull String name,
        @NotNull String lastName,
        OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {}
