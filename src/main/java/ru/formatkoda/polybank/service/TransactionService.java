package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
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
			AccountNumber accountNumber,
	        UserLogin userLogin,
	        PageRequest pageRequest
	) {
		UserEntity user = userRepository
				.findUserByLogin(userLogin.value())
				.orElseThrow(() -> new RuntimeException("user not found")); // todo change exception

		AccountEntity account = accountRepository
				.findByNumberAndUserId(accountNumber, user.id())
				.orElseThrow(() -> new AccessDeniedException("account does not belong to current user"));

		return transactionRepository.findViewsByAccountId(account.id(), pageRequest);
	}
}
