package ru.formatkoda.polybank.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.exception.UserNotFoundException;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.security.UserSession;

import java.util.Objects;

import java.math.BigDecimal;

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
				.orElseThrow(() -> new BusinessLogicException("user not found"));

		AccountEntity account = accountRepository
				.findByNumber(accountNumber)
				.orElseThrow(() -> new BusinessLogicException("account not found"));

		if (!account.userId().equals(user.id()))
			throw new BusinessLogicException("account does not belong to user");

		return account;
	}

	@Transactional
	public AccountEntity topUpOwnedAccount(
			@NonNull AccountNumber accountNumber,
			@NonNull BigDecimal amount,
			@NonNull UserLogin userLogin
	) {
		if (amount.signum() <= 0)
			throw new BusinessLogicException("amount must be greater than 0");

		findOwnedAccount(accountNumber, userLogin);

		return changeBalanceOrThrow(accountNumber, amount);
	}

	@Transactional
	public AccountEntity withdrawFromOwnedAccount(
			@NonNull AccountNumber accountNumber,
			@NonNull BigDecimal amount,
			@NonNull UserLogin userLogin
	) {
		if (amount.signum() <= 0)
			throw new BusinessLogicException("amount must be greater than 0");

		findOwnedAccount(accountNumber, userLogin);

		return changeBalanceOrThrow(accountNumber, amount.negate());
	}

	private AccountEntity changeBalanceOrThrow(@NonNull AccountNumber accountNumber, @NonNull BigDecimal delta) {
		return accountRepository
				.changeAccountBalance(accountNumber, delta)
				.orElseThrow(() -> diagnoseBalanceChangeFailure(accountNumber, delta));
	}

	private RuntimeException diagnoseBalanceChangeFailure(AccountNumber accountNumber, BigDecimal delta) {
		AccountEntity account = accountRepository.findByNumber(accountNumber)
				.orElseThrow(() -> new BusinessLogicException("account not found"));

		if (!AccountEntity.Status.ACTIVE.equals(account.status()))
			throw new BusinessLogicException("account is inactive");

		if (account.balance().add(delta).compareTo(BigDecimal.ZERO) < 0)
			throw new BusinessLogicException("insufficient funds");

		throw new BusinessLogicException("unknown exception");
	}
}
