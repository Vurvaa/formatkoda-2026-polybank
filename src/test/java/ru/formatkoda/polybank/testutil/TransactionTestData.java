package ru.formatkoda.polybank.testutil;

import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.user.UserLogin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class TransactionTestData {
	public static final AccountNumber ACCOUNT_NUMBER = new AccountNumber("12345678901234567890");
	public static final UserLogin USER_LOGIN = new UserLogin("user");
	public static final BigDecimal AMOUNT = new BigDecimal("100.00");
	public static final OffsetDateTime CREATED_AT = OffsetDateTime.of(2026, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC);

	private TransactionTestData() {
	}

	public static AccountEntity account() {
		return new AccountEntity(
				1L,
				ACCOUNT_NUMBER,
				10L,
				new BigDecimal("300.00")
		);
	}
}