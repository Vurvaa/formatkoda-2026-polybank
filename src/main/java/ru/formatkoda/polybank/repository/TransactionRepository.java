package ru.formatkoda.polybank.repository;

import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

@Repository
public class TransactionRepository {

	public PageResult<TransactionEntity> findAllByAccountNumber(AccountNumber number, PageRequest pageRequest) {
		return new PageResult<>( // todo implement repo when jooq init task is merged
				null,
				pageRequest.page(),
				pageRequest.size(),
				0);
	}
}
