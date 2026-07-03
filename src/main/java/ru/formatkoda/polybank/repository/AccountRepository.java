package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static ru.formatkoda.polybank.domain.account.AccountEntity.ACCOUNT_NUMBER_PREFIX;
import static ru.formatkoda.polybank.jooq.generated.tables.Accounts.ACCOUNTS;

@Repository
@RequiredArgsConstructor
public class AccountRepository {
	private final DSLContext dsl;
	private static final String SEQUENCE_NAME = "account_number_seq";

	public Optional<AccountEntity> createAccountForUser(Long userId, AccountEntity.Type accountType) {
		return dsl
				.insertInto(ACCOUNTS)
				.set(ACCOUNTS.NUMBER, generateNextAccountNumber())
				.set(ACCOUNTS.USER_ID, userId)
				.set(ACCOUNTS.TYPE, accountType.name())
				.set(ACCOUNTS.STATUS, AccountEntity.Status.ACTIVE.name())
				.returning()
				.fetchOptional(this::toEntity);
	}

	public List<AccountEntity> findAllByUserId(Long id) {
		return dsl
				.selectFrom(ACCOUNTS)
				.where(ACCOUNTS.USER_ID.eq(id))
				.fetch(this::toEntity);
	}

	public Optional<AccountEntity> findByNumber(AccountNumber number) {
		return dsl
				.selectFrom(ACCOUNTS)
				.where(ACCOUNTS.NUMBER.eq(number.value()))
				.fetchOptional(this::toEntity);
	}

	public Optional<AccountEntity> changeAccountBalance(AccountNumber accountNumber, BigDecimal delta) {
		var newBalanceExpression = ACCOUNTS.BALANCE.add(delta);

		return dsl
				.update(ACCOUNTS)
				.set(ACCOUNTS.BALANCE, newBalanceExpression)
				.where(ACCOUNTS.NUMBER.eq(accountNumber.value()))
				.and(ACCOUNTS.STATUS.eq(AccountEntity.Status.ACTIVE.name()))
				.and(newBalanceExpression.ge(BigDecimal.ZERO))
				.returning()
				.fetchOptional(this::toEntity);
	}

	private String generateNextAccountNumber() {
		BigInteger value = dsl.nextval(SEQUENCE_NAME);

		return ACCOUNT_NUMBER_PREFIX + String.format("%016d", value);
	}

	private AccountEntity toEntity(Record r) {
		return new AccountEntity(
				r.get(ACCOUNTS.ID),
				new AccountNumber(r.get(ACCOUNTS.NUMBER)),
				r.get(ACCOUNTS.USER_ID),
				r.get(ACCOUNTS.BALANCE),
				AccountEntity.Type.valueOf(r.get(ACCOUNTS.TYPE)),
				AccountEntity.Status.valueOf(r.get(ACCOUNTS.STATUS)),
				r.get(ACCOUNTS.CREATED_AT)
		);
	}
}
