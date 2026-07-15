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

import java.util.List;
import java.util.Optional;

import static ru.formatkoda.polybank.jooq.generated.Tables.ACCOUNTS;
import static ru.formatkoda.polybank.jooq.generated.Tables.TRANSACTIONS;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {
	private final DSLContext dsl;

	public List<TransactionWithAccountNumbersView> findViewsByAccountId(Long accountId, PageRequest pageRequest) {
		var fromAccount = ACCOUNTS.as("from_account");
		var toAccount = ACCOUNTS.as("to_account");

		var condition = TRANSACTIONS.FROM_ACCOUNT_ID.eq(accountId).or(TRANSACTIONS.TO_ACCOUNT_ID.eq(accountId));

		return dsl
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
	}

	public Long countAll(Long accountId) {
		return dsl.selectCount()
				.from(TRANSACTIONS)
				.where(TRANSACTIONS.FROM_ACCOUNT_ID.eq(accountId)
						.or(TRANSACTIONS.TO_ACCOUNT_ID.eq(accountId)))
				.fetchOne(0, Long.class);
	}

	public TransactionEntity save(TransactionEntity transaction) {
		return dsl
				.insertInto(TRANSACTIONS)
				.set(TRANSACTIONS.FROM_ACCOUNT_ID, transaction.fromAccountId())
				.set(TRANSACTIONS.TO_ACCOUNT_ID, transaction.toAccountId())
				.set(TRANSACTIONS.AMOUNT, transaction.amount())
				.set(TRANSACTIONS.TYPE, transaction.type().name())
				.set(TRANSACTIONS.STATUS, transaction.status().name())
				.set(TRANSACTIONS.CREATED_AT, transaction.createdAt())
				.returning()
				.fetchOne(this::toEntity);
	}

	public Optional<TransactionEntity> findById(long id) {
		return dsl
				.selectFrom(TRANSACTIONS)
				.where(TRANSACTIONS.ID.eq(id))
				.fetchOptional(this::toEntity);
	}

	public Optional<TransactionEntity> changeTransactionStatus(
			long id,
			TransactionEntity.Status expectedStatus,
			TransactionEntity.Status status
	) {
		return dsl
				.update(TRANSACTIONS)
				.set(TRANSACTIONS.STATUS, status.name())
				.where(TRANSACTIONS.ID.eq(id).and(TRANSACTIONS.STATUS.eq(expectedStatus.name())))
				.returning()
				.fetchOptional(this::toEntity);
	}

	private TransactionWithAccountNumbersView toTransactionWithAccountNumbersView(
			Record transactionRecord,
			Accounts fromAccount,
			Accounts toAccount
	) {
		String fromNumber = transactionRecord.get(fromAccount.NUMBER);
		String toNumber = transactionRecord.get(toAccount.NUMBER);

		return new TransactionWithAccountNumbersView(
				transactionRecord.get(TRANSACTIONS.ID),
				fromNumber == null ? null : new AccountNumber(fromNumber),
				toNumber == null ? null : new AccountNumber(toNumber),
				transactionRecord.get(TRANSACTIONS.AMOUNT),
				TransactionEntity.Type.valueOf(transactionRecord.get(TRANSACTIONS.TYPE)),
				TransactionEntity.Status.valueOf(transactionRecord.get(TRANSACTIONS.STATUS)),
				transactionRecord.get(TRANSACTIONS.CREATED_AT)
		);
	}

	private TransactionEntity toEntity(Record transactionRecord) {
		return new TransactionEntity(
				transactionRecord.get(TRANSACTIONS.ID),
				transactionRecord.get(TRANSACTIONS.FROM_ACCOUNT_ID),
				transactionRecord.get(TRANSACTIONS.TO_ACCOUNT_ID),
				transactionRecord.get(TRANSACTIONS.AMOUNT),
				TransactionEntity.Type.valueOf(transactionRecord.get(TRANSACTIONS.TYPE)),
				TransactionEntity.Status.valueOf(transactionRecord.get(TRANSACTIONS.STATUS)),
				transactionRecord.get(TRANSACTIONS.CREATED_AT)
		);
	}
}
