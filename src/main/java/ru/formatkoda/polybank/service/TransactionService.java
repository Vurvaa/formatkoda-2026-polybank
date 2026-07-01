package ru.formatkoda.polybank.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.domain.transaction.exception.InvalidTransactionException;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.repository.TransactionRepository;
import ru.formatkoda.polybank.util.mappers.TransactionMapper;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionService {
	private final TransactionRepository transactionRepository;
	private final AccountService accountService;
	private final AccountRepository accountRepository;

	public PageResult<TransactionWithAccountNumbersView> findByAccountNumber(
			@NonNull AccountNumber accountNumber,
			@NonNull UserLogin userLogin,
			@NonNull PageRequest pageRequest
	) {
		AccountEntity account = accountService.findOwnedAccount(accountNumber, userLogin);

		return transactionRepository.findViewsByAccountId(account.id(), pageRequest);
	}

	@Transactional
	public TransactionWithAccountNumbersView topUp(
			@NonNull AccountNumber accountNumber,
			@NonNull BigDecimal amount,
			@NonNull UserLogin userLogin
	) {
		validateAmount(amount);

		AccountEntity account = accountRepository
				.changeOwnedActiveAccountBalance(accountNumber, userLogin, amount)
				.orElseThrow(() -> new InvalidTransactionException("account is not available for top up"));

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
		validateAmount(amount);

		AccountEntity account = accountRepository
				.changeOwnedActiveAccountBalance(accountNumber, userLogin, amount.negate())
				.orElseThrow(() -> new InvalidTransactionException("account is not available for withdrawal"));

		TransactionEntity transaction = saveCompletedTransaction(
				account.id(),
				null,
				amount,
				TransactionEntity.Type.WITHDRAWAL
		);

		return TransactionMapper.toView(transaction, accountNumber, null);
	}

	private void validateAmount(@NonNull BigDecimal amount) {
		if (amount.signum() <= 0)
			throw new InvalidTransactionException("amount must be greater than 0");
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
