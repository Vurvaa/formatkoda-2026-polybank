package ru.formatkoda.polybank.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.account.exception.AccountDoesNotBelongToCurrentUserException;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.exception.UserNotFoundException;
import ru.formatkoda.polybank.exceptions.BusinessLogicException;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.security.UserSession;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {
	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final UserService userService;
	private final AccountRepository accountRepository;

	@Transactional
	public AccountEntity createAccountForUser(
			UserSession userSession,
			AccountEntity.AccountType accountType) {
		Objects.requireNonNull(userSession);
		Objects.requireNonNull(accountType);

		UserEntity user = userService.findUserByLogin(userSession.login());
		if (user.isBlocked()) {
			throw new BusinessLogicException("user is blocked");
		}

		return accountRepository
				.createAccountForUser(
						user.id(),
						accountType
				)
				.orElseThrow(() -> new BusinessLogicException("account not created"));
	}

	public AccountEntity findOwnedAccount(@NonNull AccountNumber accountNumber, @NonNull UserLogin userLogin) {
		UserEntity user = userRepository
				.findUserByLogin(userLogin.value())
				.orElseThrow(UserNotFoundException::new);

		return accountRepository
				.findByNumberAndUserId(accountNumber, user.id())
				.orElseThrow(AccountDoesNotBelongToCurrentUserException::new);
	}
}
