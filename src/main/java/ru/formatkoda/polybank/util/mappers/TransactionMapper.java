package ru.formatkoda.polybank.util.mappers;

import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.dto.transaction.TransactionResponseDto;
import ru.formatkoda.polybank.dto.transaction.TransactionStatus;
import ru.formatkoda.polybank.dto.transaction.TransactionType;

public class TransactionMapper {
	private TransactionMapper() {
	}

	public static TransactionResponseDto toResponse(TransactionEntity entity) {
		return new TransactionResponseDto(
				entity.id(),
				AccountMapper.toResponse(entity.accountFrom()),
				AccountMapper.toResponse(entity.accountTo()),
				entity.amount(),
				TransactionType.valueOf(entity.type().name()),
				TransactionStatus.valueOf(entity.status().name()),
				entity.createdAt()
		);
	}
}
