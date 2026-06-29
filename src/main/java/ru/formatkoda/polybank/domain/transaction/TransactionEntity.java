package ru.formatkoda.polybank.domain.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionEntity(
		Long id,
		Long fromAccountId,
		Long toAccountId,
		BigDecimal amount,
		Type type,
		Status status,
		OffsetDateTime createdAt
) {
	public enum Type {
		TRANSFER,
		DEPOSIT,
		WITHDRAWAL,
		PAYMENT,
		REFUND,
		INTEREST
	}
	public enum Status {
		PENDING,
		COMPLETED,
		FAILED,
		CANCELED,
		REJECTED
	}
}
