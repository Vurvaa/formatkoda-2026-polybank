package ru.formatkoda.polybank.dto.user;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public record UserDetailsResponseDto(
        @NotNull String login,
        @NotNull String name,
        @NotNull String lastName,
        @NotNull List<String> userRoles,
        OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {}
