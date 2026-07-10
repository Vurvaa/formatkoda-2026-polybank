package ru.formatkoda.polybank.messaging.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.messaging.event.EventEnvelope;
import ru.formatkoda.polybank.messaging.event.TransactionCreatedEvent;
import ru.formatkoda.polybank.messaging.publisher.TransactionEventPublisher;

@Component
@RequiredArgsConstructor
public class KafkaTransactionEventPublisher implements TransactionEventPublisher {

	private static final String TRANSACTION_EVENTS_TOPIC = "bank.transactions";
	private static final String TRANSACTION_CREATED = "TransactionCreated";

	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Override
	public void publishTransactionCreated(TransactionWithAccountNumbersView transaction) {
		TransactionCreatedEvent event = new TransactionCreatedEvent(
				transaction.id(),
				transaction.fromAccountNumber() == null ? null : transaction.fromAccountNumber().value(),
				transaction.toAccountNumber() == null ? null : transaction.toAccountNumber().value(),
				transaction.amount(),
				transaction.type(),
				transaction.status(),
				transaction.createdAt()
		);

		kafkaTemplate.send(
				TRANSACTION_EVENTS_TOPIC,
				event.transactionId().toString(),
				new EventEnvelope<>(TRANSACTION_CREATED, event)
		);
	}
}
