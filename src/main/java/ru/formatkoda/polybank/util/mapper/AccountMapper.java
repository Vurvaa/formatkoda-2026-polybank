package ru.formatkoda.polybank.util.mapper;

import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.dto.account.AccountResponseDto;

public class AccountMapper {
	private AccountMapper() {
	}

	public static AccountResponseDto toResponse(AccountEntity accountEntity) {
		return new AccountResponseDto(
				accountEntity.number().value(),
				accountEntity.balance(),
				accountEntity.type(),
				accountEntity.status(),
				accountEntity.createdAt()
		);
	}
}
