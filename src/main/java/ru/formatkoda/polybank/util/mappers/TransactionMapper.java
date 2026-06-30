package ru.formatkoda.polybank.util.mappers;

import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.dto.transaction.TransactionResponseDto;
import ru.formatkoda.polybank.dto.transaction.TransactionStatus;
import ru.formatkoda.polybank.dto.transaction.TransactionType;

public class TransactionMapper {
	private TransactionMapper() {
	}

	public static TransactionResponseDto toResponse(TransactionWithAccountNumbersView transaction) {
		return new TransactionResponseDto(
				transaction.id(),
				transaction.fromAccountNumber() == null ? null : transaction.fromAccountNumber().value(),
				transaction.toAccountNumber() == null ? null : transaction.toAccountNumber().value(),
				transaction.amount(),
				TransactionType.valueOf(transaction.type().name()),
				TransactionStatus.valueOf(transaction.status().name()),
				transaction.createdAt()
		);
	}
}
