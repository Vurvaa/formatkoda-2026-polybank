package ru.formatkoda.polybank.messaging.publisher;

import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;

public interface TransactionEventPublisher {

	void publishTransactionCreated(TransactionWithAccountNumbersView transaction);
}
