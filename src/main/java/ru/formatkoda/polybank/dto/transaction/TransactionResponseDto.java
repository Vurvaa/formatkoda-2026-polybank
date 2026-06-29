package ru.formatkoda.polybank.dto.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponseDto(
		Long id,
		String fromAccountNumber,
		String toAccountNumber,
		BigDecimal amount,
		TransactionType type,
		TransactionStatus status,
		OffsetDateTime createdAt
) {
}
