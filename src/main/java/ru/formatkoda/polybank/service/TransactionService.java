package ru.formatkoda.polybank.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.repository.TransactionRepository;
import ru.formatkoda.polybank.util.mappers.TransactionMapper;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionService {
	private final TransactionRepository transactionRepository;
	private final AccountService accountService;

	public PageResult<TransactionWithAccountNumbersView> findByAccountNumber(
			@NonNull AccountNumber accountNumber,
			@NonNull UserLogin userLogin,
			@NonNull PageRequest pageRequest
	) {
		AccountEntity account = accountService.findOwnedAccount(accountNumber, userLogin);

		List<TransactionWithAccountNumbersView> transactions = transactionRepository
				.findViewsByAccountId(account.id(), pageRequest);

		return new PageResult<>(
				transactions,
				pageRequest.page(),
				pageRequest.size(),
				transactions.size()
		);
	}

	@Transactional
	public TransactionWithAccountNumbersView topUp(
			@NonNull AccountNumber accountNumber,
			@NonNull BigDecimal amount,
			@NonNull UserLogin userLogin
	) {
		AccountEntity account = accountService.topUpOwnedAccount(accountNumber, amount, userLogin);

		TransactionEntity transaction = saveCompletedTransaction(
				null,
				account.id(),
				amount,
				TransactionEntity.Type.DEPOSIT
		);

		return TransactionMapper.toView(transaction, null, accountNumber);
	}

	@Transactional
	public TransactionWithAccountNumbersView withdraw(
			@NonNull AccountNumber accountNumber,
			@NonNull BigDecimal amount,
			@NonNull UserLogin userLogin
	) {
		AccountEntity account = accountService.withdrawFromOwnedAccount(accountNumber, amount, userLogin);

		TransactionEntity transaction = saveCompletedTransaction(
				account.id(),
				null,
				amount,
				TransactionEntity.Type.WITHDRAWAL
		);

		return TransactionMapper.toView(transaction, accountNumber, null);
	}

	@Transactional
	public TransactionWithAccountNumbersView transfer(
			@NonNull AccountNumber fromAccountNumber,
			@NonNull AccountNumber toAccountNumber,
			@NonNull BigDecimal amount,
			@NonNull UserLogin userLogin
	) {
		if (fromAccountNumber.equals(toAccountNumber))
			throw new BusinessLogicException("accounts must be different");

		AccountEntity fromAccount = accountService.withdrawFromOwnedAccount(fromAccountNumber, amount, userLogin);
		AccountEntity toAccount = accountService.topUpOwnedAccount(toAccountNumber, amount, userLogin);

		TransactionEntity transaction = saveCompletedTransaction(
				fromAccount.id(),
				toAccount.id(),
				amount,
				TransactionEntity.Type.TRANSFER
		);

		return TransactionMapper.toView(transaction, fromAccountNumber, toAccountNumber);
	}

	private TransactionEntity saveCompletedTransaction(
			Long fromAccountId,
			Long toAccountId,
			BigDecimal amount,
			TransactionEntity.Type type
	) {
		return transactionRepository.save(
				new TransactionEntity(
						null,
						fromAccountId,
						toAccountId,
						amount,
						type,
						TransactionEntity.Status.COMPLETED,
						OffsetDateTime.now(ZoneOffset.UTC)
				)
		);
	}
}
