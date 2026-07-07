package ru.formatkoda.polybank.dto.user;

import java.time.OffsetDateTime;
import java.util.List;

public record UserDetailsResponseDto(
        String login,
        String name,
        String lastName,
        List<String> userRoles,
        OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {}
