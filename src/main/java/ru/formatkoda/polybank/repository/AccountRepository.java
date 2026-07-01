package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.user.UserLogin;

import java.math.BigDecimal;
import java.util.Optional;

import static ru.formatkoda.polybank.jooq.generated.Tables.USERS;
import static ru.formatkoda.polybank.jooq.generated.tables.Accounts.ACCOUNTS;

// todo: fix account logic when it merged
@Repository
@RequiredArgsConstructor
public class AccountRepository {
	private final DSLContext dsl;

	public Optional<AccountEntity> findByNumberAndUserId(AccountNumber number, Long userId) {
		return dsl
				.select(
						ACCOUNTS.ID,
						ACCOUNTS.NUMBER
				)
				.from(ACCOUNTS)
				.where(ACCOUNTS.USER_ID.eq(userId))
				.and(ACCOUNTS.NUMBER.eq(number.value()))
				.fetchOptional(this::toEntity);
	}

	public Optional<AccountEntity> increaseBalanceByNumberAndUserLoginIfAccountIsActive(
			AccountNumber accountNumber,
			UserLogin userLogin,
			BigDecimal amount
	) {
		return dsl.update(ACCOUNTS)
				.set(ACCOUNTS.BALANCE, ACCOUNTS.BALANCE.add(amount))
				.from(USERS)
				.where(ACCOUNTS.USER_ID.eq(USERS.ID))
				.and(ACCOUNTS.STATUS.eq(AccountEntity.Status.ACTIVE.name()))
				.and(USERS.LOGIN.eq(userLogin.value()))
				.and(ACCOUNTS.NUMBER.eq(accountNumber.value()))
				.returning(
						ACCOUNTS.ID,
						ACCOUNTS.USER_ID,
						ACCOUNTS.NUMBER,
						ACCOUNTS.BALANCE
				)
				.fetchOptional(r -> new AccountEntity(
						r.get(ACCOUNTS.ID),
						new AccountNumber(r.get(ACCOUNTS.NUMBER)),
						r.get(ACCOUNTS.USER_ID),
						r.get(ACCOUNTS.BALANCE)
				));
	}

	private AccountEntity toEntity(Record r) {
		return new AccountEntity(
				r.get(ACCOUNTS.ID),
				new AccountNumber(r.get(ACCOUNTS.NUMBER))
		);
	}

}
