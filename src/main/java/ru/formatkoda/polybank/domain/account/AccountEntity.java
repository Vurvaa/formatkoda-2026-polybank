package ru.formatkoda.polybank.domain.account;

public record AccountEntity(
		Long id,
		AccountNumber number
) {
	// simple implementation for development purposes. will be removed when account is done by @gituser549
}
