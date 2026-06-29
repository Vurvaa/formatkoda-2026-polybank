package ru.formatkoda.polybank.domain.transaction;

import ru.formatkoda.polybank.domain.account.AccountNumber;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionWithAccountNumbersView(
		Long id,
		AccountNumber fromAccountNumber,
		AccountNumber toAccountNumber,
		BigDecimal amount,
		TransactionEntity.Type type,
		TransactionEntity.Status status,
		OffsetDateTime createdAt
) {
}