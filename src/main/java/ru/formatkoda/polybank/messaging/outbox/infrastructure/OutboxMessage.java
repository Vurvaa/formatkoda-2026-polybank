package ru.formatkoda.polybank.messaging.outbox.infrastructure;

import lombok.NonNull;

import java.util.UUID;

record OutboxMessage<T>(
		@NonNull UUID id,
		@NonNull String topic,
		@NonNull String aggregateType,
		@NonNull String aggregateId,
		@NonNull String eventType,
		@NonNull T payload
) {
	public OutboxMessage {
		if (topic.isBlank())
			throw new IllegalArgumentException("topic must not be blank");

		if (topic.length() > 249)
			throw new IllegalArgumentException("topic name must contain fewer than 250 characters");
	}

	static <T> OutboxMessage<T> of(
			@NonNull String topic,
			@NonNull String aggregateType,
			@NonNull String aggregateId,
			@NonNull String eventType,
			@NonNull T payload
	) {
		return new OutboxMessage<>(
				UUID.randomUUID(),
				topic,
				aggregateType,
				aggregateId,
				eventType,
				payload
		);
	}
}
