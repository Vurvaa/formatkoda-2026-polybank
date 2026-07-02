package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.account.*;

import java.util.Objects;
import java.util.Optional;

import static ru.formatkoda.polybank.jooq.generated.tables.Accounts.ACCOUNTS;

@Repository
@RequiredArgsConstructor
public class AccountRepository {
	private final DSLContext dsl;
	private final AccountNumberGenerator accountNumberGenerator;

	public Optional<AccountEntity> createAccountForUser(Long userId, AccountEntity.AccountType accountType) {
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(accountType, "accountType must not be null");

		return dsl
				.insertInto(ACCOUNTS)
				.set(ACCOUNTS.NUMBER, accountNumberGenerator.generate())
				.set(ACCOUNTS.USER_ID, userId)
				.set(ACCOUNTS.TYPE, accountType.name())
				.set(ACCOUNTS.STATUS, AccountEntity.AccountStatus.ACTIVE.name())
				.returning()
				.fetchOptional(this::toEntity);
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
