package ru.formatkoda.polybank.messaging.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountCreatedEvent(
		UUID eventId,
		OffsetDateTime eventTime

) {
}
