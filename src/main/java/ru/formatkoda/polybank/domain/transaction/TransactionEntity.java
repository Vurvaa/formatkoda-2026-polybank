package ru.formatkoda.polybank.domain.transaction;

import lombok.NonNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionEntity(
		@NonNull Long id,
		Long fromAccountId,
		Long toAccountId,
		@NonNull BigDecimal amount,
		@NonNull Type type,
		@NonNull Status status,
		@NonNull OffsetDateTime createdAt
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
