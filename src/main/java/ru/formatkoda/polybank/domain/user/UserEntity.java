package ru.formatkoda.polybank.domain.user;

import java.time.OffsetDateTime;
import java.util.Optional;

public record UserEntity(
		Long id,
		String login,
		String name,
		String lastName,
		String passwordHash,
		OffsetDateTime createdAt,
		Optional<OffsetDateTime> blockedAt
) {
}
