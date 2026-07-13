package ru.formatkoda.polybank.messaging.outbox.infrastructure;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import static ru.formatkoda.polybank.jooq.generated.Tables.OUTBOX_EVENTS;

@Repository
@RequiredArgsConstructor
class JooqOutboxRepository {
	private final DSLContext dsl;
	private final ObjectMapper objectMapper;

	void insert(OutboxMessage<?> message) {
		String payload = objectMapper.writeValueAsString(message.payload());

		dsl.insertInto(OUTBOX_EVENTS)
				.set(OUTBOX_EVENTS.ID, message.id())
				.set(OUTBOX_EVENTS.TOPIC, message.topic())
				.set(OUTBOX_EVENTS.AGGREGATE_TYPE, message.aggregateType())
				.set(OUTBOX_EVENTS.AGGREGATE_ID, message.aggregateId())
				.set(OUTBOX_EVENTS.EVENT_TYPE, message.eventType())
				.set(OUTBOX_EVENTS.PAYLOAD, JSONB.jsonb(payload))
				.execute();
	}
}
