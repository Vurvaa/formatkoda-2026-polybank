package ru.formatkoda.polybank.messaging.event;

import java.time.OffsetDateTime;

public record UserRegisteredEvent(
		Long userId,
		String userLogin,
		String name,
		String lastName,
		String role,
		OffsetDateTime createdAt
) {
}
