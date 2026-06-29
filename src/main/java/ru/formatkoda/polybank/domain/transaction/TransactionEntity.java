package ru.formatkoda.polybank.domain.transaction;

import ru.formatkoda.polybank.domain.account.AccountEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionEntity(
		int id,
		AccountEntity accountFrom,
		AccountEntity accountTo,
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
