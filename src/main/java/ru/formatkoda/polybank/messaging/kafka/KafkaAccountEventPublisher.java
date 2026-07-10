package ru.formatkoda.polybank.messaging.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.messaging.publisher.AccountEventPublisher;

@Component
@RequiredArgsConstructor
public class KafkaAccountEventPublisher implements AccountEventPublisher {

	private static final String ACCOUNT_EVENTS_TOPIC = "account-events";

	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Override
	public void publishAccountCreated(AccountEntity account) {
		kafkaTemplate.send(
				ACCOUNT_EVENTS_TOPIC,
				account.id()
		);
	}
}
