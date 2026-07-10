package ru.formatkoda.polybank.messaging.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.messaging.event.AccountCreatedEvent;
import ru.formatkoda.polybank.messaging.event.EventEnvelope;
import ru.formatkoda.polybank.messaging.publisher.AccountEventPublisher;

@Component
@RequiredArgsConstructor
public class KafkaAccountEventPublisher implements AccountEventPublisher {

	private static final String ACCOUNT_EVENTS_TOPIC = "accounts";
	private static final String ACCOUNT_CREATED = "AccountCreated";

	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Override
	public void publishAccountCreated(AccountEntity account) {
		AccountCreatedEvent event = new AccountCreatedEvent(
				account.id(),
				account.number().value(),
				account.userId(),
				account.type(),
				account.createdAt()
		);

		kafkaTemplate.send(
				ACCOUNT_EVENTS_TOPIC,
				event.accountId().toString(),
				new EventEnvelope<>(ACCOUNT_CREATED, event)
		);
	}
}
