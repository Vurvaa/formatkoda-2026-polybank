package ru.formatkoda.polybank.util.mapper;

import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.dto.account.AccountDetailsResponseDto;

public class AccountMapper {
	private AccountMapper() {
	}

	public static AccountDetailsResponseDto toAccountDetailsResponseDto(AccountEntity accountEntity) {
		return new AccountDetailsResponseDto(
				accountEntity.number().value(),
				accountEntity.balance(),
				accountEntity.type(),
				accountEntity.status()
		);
	}
}
