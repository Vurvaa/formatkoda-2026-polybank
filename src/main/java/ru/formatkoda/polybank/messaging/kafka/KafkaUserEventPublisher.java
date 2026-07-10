package ru.formatkoda.polybank.messaging.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.messaging.event.EventEnvelope;
import ru.formatkoda.polybank.messaging.event.UserRegisteredEvent;
import ru.formatkoda.polybank.messaging.publisher.UserEventPublisher;

@Component
@RequiredArgsConstructor
public class KafkaUserEventPublisher implements UserEventPublisher {

	private static final String USER_EVENTS_TOPIC = "bank.users";
	private static final String USER_REGISTERED = "UserRegistered";

	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Override
	public void publishUserRegistered(UserEntity user, String role) {
		UserRegisteredEvent event = new UserRegisteredEvent(
				user.id(),
				user.login().value(),
				user.name(),
				user.lastName(),
				role,
				user.createdAt()
		);

		kafkaTemplate.send(
				USER_EVENTS_TOPIC,
				event.userId().toString(),
				new EventEnvelope<>(USER_REGISTERED, event)
		);
	}
}
