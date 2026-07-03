package ru.formatkoda.polybank.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {
	private final AccountRepository accountRepository;
	private final UserService userService;

	@Transactional
	public AccountEntity createAccountForUser(@NonNull AccountEntity.Type accountType, @NonNull UserLogin userLogin) {
		UserEntity user = userService.findUserByLogin(userLogin);
		if (user.isBlocked())
			throw new BusinessLogicException("user is blocked");

		return accountRepository
				.createAccountForUser(user.id(),accountType)
				.orElseThrow(() -> new BusinessLogicException("account not created"));
	}

	public List<AccountEntity> findAllForUser(@NonNull UserLogin userLogin) {
		UserEntity user = userService.findUserByLogin(userLogin);

		return accountRepository.findAllByUserId(user.id());
	}

	public AccountEntity findOwnedAccount(@NonNull AccountNumber accountNumber, @NonNull UserLogin userLogin) {
		UserEntity user = userService.findUserByLogin(userLogin);
		AccountEntity account = findAccountOrThrow(accountNumber);

		if (!account.userId().equals(user.id()))
			throw new BusinessLogicException("account does not belong to user");

		return account;
	}

	@Transactional
	public AccountEntity topUpAccount(@NonNull AccountNumber accountNumber, @NonNull BigDecimal amount) {
		validateAmount(amount);
		findAccountOrThrow(accountNumber);

		return changeBalanceOrThrow(accountNumber, amount);
	}

	@Transactional
	public AccountEntity topUpOwnedAccount(
			@NonNull AccountNumber accountNumber,
			@NonNull BigDecimal amount,
			@NonNull UserLogin userLogin
	) {
		validateAmount(amount);
		findOwnedAccount(accountNumber, userLogin);

		return changeBalanceOrThrow(accountNumber, amount);
	}

	@Transactional
	public AccountEntity withdrawFromOwnedAccount(
			@NonNull AccountNumber accountNumber,
			@NonNull BigDecimal amount,
			@NonNull UserLogin userLogin
	) {
		validateAmount(amount);
		findOwnedAccount(accountNumber, userLogin);

		return changeBalanceOrThrow(accountNumber, amount.negate());
	}

	private AccountEntity findAccountOrThrow(@NonNull AccountNumber accountNumber) {
		return accountRepository
				.findByNumber(accountNumber)
				.orElseThrow(() -> new BusinessLogicException("account not found"));
	}

	private AccountEntity changeBalanceOrThrow(@NonNull AccountNumber accountNumber, @NonNull BigDecimal delta) {
		return accountRepository
				.changeAccountBalance(accountNumber, delta)
				.orElseThrow(() -> diagnoseBalanceChangeFailure(accountNumber, delta));
	}

	private RuntimeException diagnoseBalanceChangeFailure(AccountNumber accountNumber, BigDecimal delta) {
		AccountEntity account = findAccountOrThrow(accountNumber);

		if (!AccountEntity.Status.ACTIVE.equals(account.status()))
			throw new BusinessLogicException("account is inactive");

		if (account.balance().add(delta).compareTo(BigDecimal.ZERO) < 0)
			throw new BusinessLogicException("insufficient funds");

		throw new BusinessLogicException("unknown exception");
	}

	private void validateAmount(BigDecimal amount) {
		if (amount.signum() <= 0)
			throw new BusinessLogicException("amount must be greater than 0");
	}
}
