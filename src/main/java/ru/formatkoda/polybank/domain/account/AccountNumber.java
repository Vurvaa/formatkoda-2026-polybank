package ru.formatkoda.polybank.domain.account;

import lombok.NonNull;
import ru.formatkoda.polybank.exception.BusinessLogicException;

public record AccountNumber(@NonNull String value) {
	public AccountNumber {
		if (value.length() != 20)
			throw new BusinessLogicException("account number length must be 20 digits");

		if (!value.matches("\\d{20}"))
			throw new BusinessLogicException("account number must contain only digits");
	}
}
