package ru.formatkoda.polybank.messaging.outbox.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.messaging.event.AccountCreatedEvent;
import ru.formatkoda.polybank.messaging.outbox.infrastructure.OutboxWriter;
import ru.formatkoda.polybank.messaging.publisher.AccountEventPublisher;

@Component
@RequiredArgsConstructor
public class AccountOutboxPublisher implements AccountEventPublisher {

	private static final String TOPIC = "bank.accounts";
	private static final String AGGREGATE_TYPE = "Account";

	private final OutboxWriter outboxWriter;

	@Override
	public void publishAccountCreated(AccountEntity account) {
		AccountCreatedEvent payload = new AccountCreatedEvent(
				account.id(),
				account.number().value(),
				account.userId(),
				account.type(),
				account.createdAt()
		);

		outboxWriter.append(
				TOPIC,
				AGGREGATE_TYPE,
				payload.accountId().toString(),
				AccountCreatedEvent.TYPE,
				payload
		);
	}
}
