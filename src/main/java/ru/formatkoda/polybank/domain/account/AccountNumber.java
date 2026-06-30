package ru.formatkoda.polybank.domain.account;

import lombok.NonNull;

public record AccountNumber(@NonNull String value) {
	public AccountNumber {
		if (value.length() != 20)
			throw new IllegalArgumentException("account number must contain exactly 20 characters");

		if (!value.matches("\\d+"))
			throw new IllegalArgumentException("account number must contain only digits");
	}
}
