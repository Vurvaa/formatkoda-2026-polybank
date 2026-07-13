package ru.formatkoda.polybank.messaging.outbox.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.messaging.event.TransactionCreatedEvent;
import ru.formatkoda.polybank.messaging.outbox.infrastructure.OutboxWriter;
import ru.formatkoda.polybank.messaging.publisher.TransactionEventPublisher;

@Component
@RequiredArgsConstructor
public class TransactionOutboxPublisher implements TransactionEventPublisher {

	private static final String TOPIC = "bank.transactions";
	private static final String AGGREGATE_TYPE = "Transaction";

	private final OutboxWriter outboxWriter;

	@Override
	public void publishTransactionCreated(TransactionWithAccountNumbersView transaction) {
		TransactionCreatedEvent payload = new TransactionCreatedEvent(
				transaction.id(),
				transaction.fromAccountNumber() == null ? null : transaction.fromAccountNumber().value(),
				transaction.toAccountNumber() == null ? null : transaction.toAccountNumber().value(),
				transaction.amount(),
				transaction.type(),
				transaction.status(),
				transaction.createdAt()
		);

		outboxWriter.append(
				TOPIC,
				AGGREGATE_TYPE,
				payload.transactionId().toString(),
				TransactionCreatedEvent.TYPE,
				payload
		);
	}
}
