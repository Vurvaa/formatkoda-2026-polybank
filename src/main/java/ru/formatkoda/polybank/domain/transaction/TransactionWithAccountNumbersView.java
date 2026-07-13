package ru.formatkoda.polybank.domain.transaction;

import lombok.NonNull;
import ru.formatkoda.polybank.domain.account.AccountNumber;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionWithAccountNumbersView(
		@NonNull Long id,
		AccountNumber fromAccountNumber,
		AccountNumber toAccountNumber,
		@NonNull BigDecimal amount,
		@NonNull TransactionEntity.Type type,
		@NonNull TransactionEntity.Status status,
		@NonNull OffsetDateTime createdAt
) {
}
