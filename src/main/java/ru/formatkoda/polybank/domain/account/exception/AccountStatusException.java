package ru.formatkoda.polybank.domain.account.exception;

public class AccountStatusException extends RuntimeException {
	public AccountStatusException(String message) {
		super(message);
	}
}
