package ru.formatkoda.polybank.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.account.exception.AccountDoesNotBelongToCurrentUserException;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.exception.UserNotFoundException;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.repository.TransactionRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionService {
	private final TransactionRepository transactionRepository;
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;

	public PageResult<TransactionWithAccountNumbersView> findByAccountNumber(
			@NonNull AccountNumber accountNumber,
	        @NonNull UserLogin userLogin,
	        @NonNull PageRequest pageRequest
	) {
		UserEntity user = userRepository
				.findUserByLogin(userLogin.value())
				.orElseThrow(UserNotFoundException::new);

		AccountEntity account = accountRepository
				.findByNumberAndUserId(accountNumber, user.id())
				.orElseThrow(AccountDoesNotBelongToCurrentUserException::new);

		return transactionRepository.findViewsByAccountId(account.id(), pageRequest);
	}
}
