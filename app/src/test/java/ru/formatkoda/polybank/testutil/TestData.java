package ru.formatkoda.polybank.testutil;

import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public final class TestData {
	public static final AccountNumber ACCOUNT_NUMBER = new AccountNumber("12345678901234567890");
	public static final UserLogin USER_LOGIN = new UserLogin("user");
	public static final BigDecimal AMOUNT = new BigDecimal("100.00");
	public static final OffsetDateTime CREATED_AT = OffsetDateTime.of(2026, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC);

	public static final UserLogin SENIOR_MANAGER_LOGIN = new UserLogin("senior_manager");

	private TestData() {
	}

	public static AccountEntity account(long userId) {
		return new AccountEntity(
				1L,
				ACCOUNT_NUMBER,
				userId,
				AMOUNT,
				AccountEntity.Type.CURRENT,
				AccountEntity.Status.ACTIVE,
				CREATED_AT
		);
	}

	public static AccountEntity account() {
		return account(10L);
	}

	public static UserEntity user() {
		return new UserEntity(
				10L,
				USER_LOGIN,
				"User",
				"Test",
				"password-hash",
				CREATED_AT,
				null
		);
	}

	public static UserEntity userManager() {
		return new UserEntity(
				25L,
				SENIOR_MANAGER_LOGIN,
				"User",
				"Test",
				"password-hash",
				CREATED_AT,
				null
		);
	}

	public static UserWithRolesView userWithRoles() {
		return new UserWithRolesView(
				25L,
				USER_LOGIN,
				"User",
				"Test",
				List.of("CLIENT"),
				CREATED_AT,
				null
		);
	}
}