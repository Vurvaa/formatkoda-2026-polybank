package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

import static ru.formatkoda.polybank.jooq.generated.tables.Transactions.TRANSACTIONS;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {
	private final DSLContext dsl;

	public PageResult<TransactionEntity> findAllByAccountNumber(AccountNumber number, PageRequest pageRequest) {
		dsl.select(TRANSACTIONS.ID)

		return new PageResult<>(
				null,
				pageRequest.page(),
				pageRequest.size(),
				0);
	}

	private TransactionEntity toEntity() {
		return null;
	}
}
