package ru.formatkoda.polybank.messaging.outbox.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.messaging.event.UserRegisteredEvent;
import ru.formatkoda.polybank.messaging.outbox.infrastructure.OutboxWriter;
import ru.formatkoda.polybank.messaging.publisher.UserEventPublisher;

@Component
@RequiredArgsConstructor
public class UserOutboxPublisher implements UserEventPublisher {

	private static final String TOPIC = "bank.users";
	private static final String AGGREGATE_TYPE = "User";

	private final OutboxWriter outboxWriter;

	@Override
	public void publishUserRegistered(UserEntity user, String role) {
		UserRegisteredEvent payload = new UserRegisteredEvent(
				user.id(),
				user.login().value(),
				user.name(),
				user.lastName(),
				role,
				user.createdAt()
		);

		outboxWriter.append(
				TOPIC,
				AGGREGATE_TYPE,
				payload.userId().toString(),
				UserRegisteredEvent.TYPE,
				payload
		);
	}
}
