package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;

import java.util.Optional;

import static ru.formatkoda.polybank.jooq.generated.tables.Accounts.ACCOUNTS;

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

	private AccountEntity toEntity(Record r) {
		return new AccountEntity(
				r.get(ACCOUNTS.ID),
				new AccountNumber(r.get(ACCOUNTS.NUMBER))
		);
	}
}
