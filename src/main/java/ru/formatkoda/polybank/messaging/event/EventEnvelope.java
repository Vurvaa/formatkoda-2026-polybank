package ru.formatkoda.polybank.messaging.event;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record EventEnvelope<T>(
		UUID eventId,
		OffsetDateTime eventTime,
		String eventType,
		T payload
) {
	public EventEnvelope(String eventType, T payload) {
		this(
				UUID.randomUUID(),
				OffsetDateTime.now(ZoneOffset.UTC),
				eventType,
				payload
		);
	}
}
