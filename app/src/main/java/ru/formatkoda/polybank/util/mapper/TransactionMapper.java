package ru.formatkoda.polybank.util.mapper;

import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
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
				transaction.amount().toPlainString(),
				TransactionType.valueOf(transaction.type().name()),
				TransactionStatus.valueOf(transaction.status().name()),
				transaction.createdAt()
		);
	}

	public static TransactionWithAccountNumbersView toView(
			TransactionEntity transaction,
			AccountNumber fromAccountNumber,
			AccountNumber toAccountNumber
	) {
		return new TransactionWithAccountNumbersView(
				transaction.id(),
				fromAccountNumber,
				toAccountNumber,
				transaction.amount(),
				transaction.type(),
				transaction.status(),
				transaction.createdAt()
		);
	}
}
