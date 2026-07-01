package ru.formatkoda.polybank.domain.account;

import java.math.BigDecimal;

public record AccountEntity(
		Long id,
		AccountNumber number,
		Long userId,
		Status status,
		BigDecimal balance
) {
	public AccountEntity(Long id, AccountNumber number) {
		this(id, number, 0L, Status.ACTIVE, BigDecimal.ZERO);
	}

	public AccountEntity(Long id, AccountNumber number, Long userId, BigDecimal balance) {
		this(id, number, userId, Status.ACTIVE, balance);
	}

	public enum Status {
		ACTIVE,
		BLOCKED
	}

	public boolean isActive() {
		return status == Status.ACTIVE;
	}
	// simple implementation for development purposes. will be removed when account is done by @gituser549
}
