package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.jooq.generated.tables.Accounts;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

import java.util.List;

import static ru.formatkoda.polybank.jooq.generated.Tables.ACCOUNTS;
import static ru.formatkoda.polybank.jooq.generated.Tables.TRANSACTIONS;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {
	private final DSLContext dsl;

	public PageResult<TransactionWithAccountNumbersView> findViewsByAccountId(long accountId, PageRequest pageRequest) {
		var fromAccount = ACCOUNTS.as("from_account");
		var toAccount = ACCOUNTS.as("to_account");

		var condition = TRANSACTIONS.FROM_ACCOUNT_ID.eq(accountId).or(TRANSACTIONS.TO_ACCOUNT_ID.eq(accountId));

		List<TransactionWithAccountNumbersView> transactions = dsl
				.select(
						TRANSACTIONS.ID,
						fromAccount.NUMBER,
						toAccount.NUMBER,
						TRANSACTIONS.AMOUNT,
						TRANSACTIONS.TYPE,
						TRANSACTIONS.STATUS,
						TRANSACTIONS.CREATED_AT
				)
				.from(TRANSACTIONS)
				.leftJoin(fromAccount).on(fromAccount.ID.eq(TRANSACTIONS.FROM_ACCOUNT_ID))
				.leftJoin(toAccount).on(toAccount.ID.eq(TRANSACTIONS.TO_ACCOUNT_ID))
				.where(condition)
				.orderBy(TRANSACTIONS.CREATED_AT.desc(), TRANSACTIONS.ID.desc())
				.limit(pageRequest.size())
				.offset(pageRequest.offset())
				.fetch(r -> toTransactionWithAccountNumbersView(r, fromAccount, toAccount));

		Long total = dsl
				.selectCount()
				.from(TRANSACTIONS)
				.where(condition)
				.fetchOne(0, Long.class);

		return new PageResult<>(
				transactions,
				pageRequest.page(),
				pageRequest.size(),
				total == null ? 0 : total
		);
	}

	private TransactionWithAccountNumbersView toTransactionWithAccountNumbersView(
			Record r,
			Accounts fromAccount,
			Accounts toAccount
	) {
		String fromNumber = r.get(fromAccount.NUMBER);
		String toNumber = r.get(toAccount.NUMBER);

		return new TransactionWithAccountNumbersView(
				r.get(TRANSACTIONS.ID),
				fromNumber == null ? null : new AccountNumber(fromNumber),
				toNumber == null ? null : new AccountNumber(toNumber),
				r.get(TRANSACTIONS.AMOUNT),
				TransactionEntity.Type.valueOf(r.get(TRANSACTIONS.TYPE)),
				TransactionEntity.Status.valueOf(r.get(TRANSACTIONS.STATUS)),
				r.get(TRANSACTIONS.CREATED_AT)
		);
	}
}
