package ru.formatkoda.polybank.util.mappers;

import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.dto.CreateAccountResponseDto;

public class AccountMapper {
	private AccountMapper() {
	}

	public static CreateAccountResponseDto toCreateAccountResponseDto(AccountEntity accountEntity) {
		return new CreateAccountResponseDto(
				accountEntity.id(),
				accountEntity.number(),
				accountEntity.balance(),
				accountEntity.type(),
				accountEntity.status(),
				accountEntity.createdAt()
		);
	}
}
