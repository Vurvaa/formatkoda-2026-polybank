package ru.formatkoda.polybank.domain.account.exception;

public class AccountDoesNotBelongToCurrentUserException extends RuntimeException {
	public AccountDoesNotBelongToCurrentUserException() {
		super();
	}
}
