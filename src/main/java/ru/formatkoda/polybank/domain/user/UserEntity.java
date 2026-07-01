package ru.formatkoda.polybank.domain.user;

import lombok.NonNull;

import java.time.OffsetDateTime;

public record UserEntity(
		@NonNull Long id,
		@NonNull String login,
		@NonNull String name,
		@NonNull String lastName,
		@NonNull String passwordHash,
		@NonNull OffsetDateTime createdAt,
		OffsetDateTime blockedAt
) {
}
