package ru.formatkoda.polybank.domain.account;

import ru.formatkoda.polybank.exceptions.BusinessLogicException;

public record AccountNumber(String value) {
	public AccountNumber {
		if (value == null)
			throw new BusinessLogicException("account number must not be null");

		if (value.length() != 20) {
			throw new BusinessLogicException("account number length must be 20 digits");
		}

		if (!value.matches("\\d{20}"))
			throw new BusinessLogicException("account number must contain only digits");
	}
}
