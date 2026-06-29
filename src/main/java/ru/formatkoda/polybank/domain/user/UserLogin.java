package ru.formatkoda.polybank.domain.user;

public record UserLogin(String value) {
	public UserLogin {
		if (value == null)
			throw new IllegalArgumentException("user login must not be null");
	}
}
