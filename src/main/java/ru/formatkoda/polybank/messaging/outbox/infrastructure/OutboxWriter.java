package ru.formatkoda.polybank.messaging.outbox.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OutboxWriter {

	private final JooqOutboxRepository repository;

	@Transactional(propagation = Propagation.MANDATORY)
	public void append(
			String topic,
			String aggregateType,
			String aggregateId,
			String eventType,
			Object payload
	) {
		OutboxMessage<Object> message = OutboxMessage.of(
				topic,
				aggregateType,
				aggregateId,
				eventType,
				payload
		);

		repository.insert(message);
	}
}
