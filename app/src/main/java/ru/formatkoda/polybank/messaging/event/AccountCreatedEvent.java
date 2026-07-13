package ru.formatkoda.polybank.messaging.event;

import ru.formatkoda.polybank.domain.account.AccountEntity;

import java.time.OffsetDateTime;

public record AccountCreatedEvent(
		Long accountId,
		String accountNumber,
		Long userId,
		AccountEntity.Type accountType,
		OffsetDateTime createdAt
) {
	public static final String TYPE = "AccountCreated";
}
