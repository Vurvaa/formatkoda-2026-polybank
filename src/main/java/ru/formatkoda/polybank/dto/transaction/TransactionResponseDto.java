package ru.formatkoda.polybank.dto.transaction;

import ru.formatkoda.polybank.dto.account.AccountResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponseDto(
		int id,
		AccountResponse accountFrom,
		AccountResponse accountTo,
		BigDecimal amount,
		TransactionType type,
		TransactionStatus status,
		OffsetDateTime createdAt
) {
}
