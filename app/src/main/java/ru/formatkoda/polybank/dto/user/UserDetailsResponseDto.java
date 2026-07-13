package ru.formatkoda.polybank.dto.user;

import lombok.NonNull;
import ru.formatkoda.polybank.domain.user.UserLogin;

import java.time.OffsetDateTime;
import java.util.List;

public record UserDetailsResponseDto(
        @NonNull UserLogin login,
        @NonNull String name,
        @NonNull String lastName,
        @NonNull List<String> roles,
        @NonNull OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {
}
