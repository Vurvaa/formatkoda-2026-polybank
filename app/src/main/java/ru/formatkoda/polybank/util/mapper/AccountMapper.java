package ru.formatkoda.polybank.util.mapper;

import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountInfo;
import ru.formatkoda.polybank.dto.account.AccountResponseDto;
import ru.formatkoda.polybank.messaging.dto.AccountNotificationEventDto;

public class AccountMapper {
	private AccountMapper() {
	}

	public static AccountResponseDto toResponse(AccountEntity accountEntity) {
		return new AccountResponseDto(
				accountEntity.number().value(),
				accountEntity.balance().toPlainString(),
				accountEntity.type(),
				accountEntity.status(),
				accountEntity.createdAt()
		);
	}

	public static AccountInfo toAccountInfo(AccountEntity accountEntity) {
		return new AccountInfo(
				accountEntity.number(),
				accountEntity.type(),
				accountEntity.status(),
				accountEntity.createdAt()
		);
	}

	public static AccountNotificationEventDto toNotificationDto(
			AccountEntity accountEntity,
			String name,
			String email
	) {
		return new AccountNotificationEventDto(
				accountEntity.id(),
				accountEntity.number().value(),
				accountEntity.userId(),
				name,
				email,
				accountEntity.balance(),
				accountEntity.type().name(),
				accountEntity.status().name(),
				accountEntity.createdAt()
		);
	}
}
