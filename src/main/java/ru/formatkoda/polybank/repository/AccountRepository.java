package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.account.*;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import static ru.formatkoda.polybank.domain.account.AccountEntity.ACCOUNT_NUMBER_PREFIX;
import static ru.formatkoda.polybank.jooq.generated.tables.Accounts.ACCOUNTS;

import static ru.formatkoda.polybank.jooq.generated.tables.Users.USERS;
import static ru.formatkoda.polybank.jooq.generated.tables.Accounts.ACCOUNTS;

@Repository
@RequiredArgsConstructor
public class AccountRepository {
	private final DSLContext dsl;
	private static final String SEQUENCE_NAME = "account_number_seq";

	public Optional<AccountEntity> createAccountForUser(Long userId, AccountEntity.AccountType accountType) {
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(accountType, "accountType must not be null");

		return dsl
				.insertInto(ACCOUNTS)
				.set(ACCOUNTS.NUMBER, generateNextAccountNumber())
				.set(ACCOUNTS.USER_ID, userId)
				.set(ACCOUNTS.TYPE, accountType.name())
				.set(ACCOUNTS.STATUS, AccountEntity.AccountStatus.ACTIVE.name())
				.returning()
				.fetchOptional(this::toEntity);
	}

	public boolean existsByNumberAndUserLogin(AccountNumber number, UserLogin login) {
		return dsl.fetchExists(
				dsl.selectOne()
						.from(ACCOUNTS)
						.join(USERS).on(ACCOUNTS.USER_ID.eq(USERS.ID))
						.where(ACCOUNTS.NUMBER.eq(number.value()))
						.and(USERS.LOGIN.eq(login.value()))
		);
	}

	public String generateNextAccountNumber() {
		BigInteger value = dsl.nextval(SEQUENCE_NAME);

		return ACCOUNT_NUMBER_PREFIX + String.format("%016d", value);
	}

	private AccountEntity toEntity(Record r) {
		return new AccountEntity(
				r.get(ACCOUNTS.ID),
				new AccountNumber(r.get(ACCOUNTS.NUMBER)),
				r.get(ACCOUNTS.USER_ID),
				r.get(ACCOUNTS.BALANCE),
				AccountEntity.AccountType.valueOf(r.get(ACCOUNTS.TYPE)),
				AccountEntity.AccountStatus.valueOf(r.get(ACCOUNTS.STATUS)),
				r.get(ACCOUNTS.CREATED_AT)
		);
	}
}
