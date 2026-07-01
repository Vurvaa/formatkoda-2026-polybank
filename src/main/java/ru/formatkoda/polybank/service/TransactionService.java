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
		if (amount.signum() <= 0)
			throw new InvalidTransactionException("amount must be greater than 0");

		AccountEntity account = accountRepository
				.increaseBalanceByNumberAndUserLoginIfAccountIsActive(accountNumber, userLogin, amount)
				.orElseThrow(() -> new InvalidTransactionException("account is not available for top up"));

		TransactionEntity transaction = transactionRepository.save(
				new TransactionEntity(
						null,
						null,
						account.id(),
						amount,
						TransactionEntity.Type.DEPOSIT,
						TransactionEntity.Status.COMPLETED,
						OffsetDateTime.now(ZoneOffset.UTC)
				)
		);

		return TransactionMapper.toTopUpView(transaction, accountNumber);
	}
}
