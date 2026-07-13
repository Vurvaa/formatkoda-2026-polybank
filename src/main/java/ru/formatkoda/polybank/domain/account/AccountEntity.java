package ru.formatkoda.polybank.domain.account;

import lombok.NonNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountEntity(
		Long id,
		@NonNull AccountNumber number,
		@NonNull Long userId,
		@NonNull BigDecimal balance,
		@NonNull Type type,
		@NonNull Status status,
		@NonNull OffsetDateTime createdAt
) {
	public static final String ACCOUNT_NUMBER_PREFIX = "6767";

	public enum Status {
		ACTIVE,
		FROZEN,
		BLOCKED,
		CLOSED
	}

	public enum Type {
		CURRENT,
		FIXED_DEPOSIT,
		SAVINGS,
		CREDIT
	}
}
