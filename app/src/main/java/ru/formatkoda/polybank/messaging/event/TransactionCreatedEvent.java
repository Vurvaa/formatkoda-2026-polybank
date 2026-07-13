package ru.formatkoda.polybank.messaging.event;

import ru.formatkoda.polybank.domain.transaction.TransactionEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionCreatedEvent(
		Long transactionId,
		String fromAccountNumber,
		String toAccountNumber,
		BigDecimal amount,
		TransactionEntity.Type transactionType,
		TransactionEntity.Status transactionStatus,
		OffsetDateTime createdAt
) {
	public static final String TYPE = "TransactionCreated";
}
