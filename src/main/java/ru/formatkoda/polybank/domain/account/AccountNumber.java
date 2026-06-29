package ru.formatkoda.polybank.domain.account;

public record AccountNumber(String value) {
	public AccountNumber {
		if (value == null)
			throw new IllegalArgumentException("account number must not be null");

		if (value.length() != 20)
			throw new IllegalArgumentException("account number must contain exactly 20 characters");

		if (!value.matches("\\d+"))
			throw new IllegalArgumentException("account number must contain only digits");
	}
}
