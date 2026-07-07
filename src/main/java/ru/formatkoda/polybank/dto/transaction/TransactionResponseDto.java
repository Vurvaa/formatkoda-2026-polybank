package ru.formatkoda.polybank.dto.transaction;

import java.time.OffsetDateTime;

public record TransactionResponseDto(
		Long id,
		String fromAccountNumber,
		String toAccountNumber,
		String amount,
		TransactionType type,
		TransactionStatus status,
		OffsetDateTime createdAt
) {
}
