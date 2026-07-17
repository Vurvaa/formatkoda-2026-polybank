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
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.messaging.publisher.AccountEventPublisher;
import ru.formatkoda.polybank.messaging.publisher.NotificationEventPublisher;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.util.mapper.AccountMapper;

import static ru.formatkoda.polybank.messaging.outbox.publisher.NotificationOutboxPublisher.AvailableNotificationMethods.EMAIL;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {
	private final AccountRepository accountRepository;
	private final UserService userService;
	private final AccountEventPublisher accountEventPublisher;
	private final NotificationEventPublisher notificationEventPublisher;

	@Transactional
	public AccountEntity createAccountForUser(@NonNull AccountEntity.Type accountType, @NonNull UserLogin userLogin) {
		UserEntity user = userService.findNotBlockedUserByLogin(userLogin);

		AccountEntity account = accountRepository
				.createAccountForUser(user.id(), accountType)
				.orElseThrow(() -> new BusinessLogicException("account not created"));

		accountEventPublisher.publishAccountCreated(account);
		notificationEventPublisher.publishAccountNotificationEvent(
				AccountMapper.toNotificationDto(
						account,
						user.name(),
						user.email().value()
				),
				List.of(EMAIL.name()),
				"ACCOUNT_CREATED"
		);

		return account;
	}

	public List<AccountEntity> findAllForUser(@NonNull UserLogin userLogin) {
		UserEntity user = userService.findUserByLogin(userLogin);

		return accountRepository.findAllByUserId(user.id());
	}

	public AccountEntity findAccountByAccountNumber(
			@NonNull AccountNumber accountNumber
	) {
		return findAccountOrThrow(accountNumber);
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
	public AccountEntity withdrawFromAccount(
			@NonNull AccountNumber accountNumber,
			@NonNull BigDecimal amount) {
		validateAmount(amount);
		findAccountOrThrow(accountNumber);

		return changeBalanceOrThrow(accountNumber, amount.negate());
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

	@Transactional
	public AccountEntity closeAccountForUser(
			@NonNull UserLogin userLogin,
			@NonNull AccountNumber accountNumber
	) {
		UserEntity user = userService.findNotBlockedUserByLogin(userLogin);

		AccountEntity account = findAccountOrThrow(accountNumber);

		if (!account.userId().equals(user.id())) {
			throw new BusinessLogicException("account does not belong to this user");
		} else if (account.balance().signum() != 0) {
			throw new BusinessLogicException("account balance is not zero");
		}

		if (account.status().equals(AccountEntity.Status.CLOSED)) {
			return account;
		} else if (!account.status().equals(AccountEntity.Status.ACTIVE)) {
			throw new BusinessLogicException("account is not active");
		}

		return accountRepository
				.changeAccountStatus(accountNumber, AccountEntity.Status.CLOSED)
				.orElseThrow(
						() -> new BusinessLogicException("account not closed")
				);
	}

	@Transactional
	public AccountEntity freezeAccountForUser(
			@NonNull UserLogin userLogin,
			@NonNull AccountNumber accountNumber
	) {
		UserEntity user = userService.findNotBlockedUserByLogin(userLogin);

		AccountEntity account = findAccountOrThrow(accountNumber);

		if (!account.userId().equals(user.id())) {
			throw new BusinessLogicException("account does not belong to this user");
		} else if (account.type().equals(AccountEntity.Type.CREDIT)) {
			throw new BusinessLogicException("account type is credit");
		}

		if (account.status().equals(AccountEntity.Status.FROZEN)) {
			return account;
		} else if (!account.status().equals(AccountEntity.Status.ACTIVE)) {
			throw new BusinessLogicException("account is not active");
		}

		return accountRepository
				.changeAccountStatus(accountNumber, AccountEntity.Status.FROZEN)
				.orElseThrow(() -> new BusinessLogicException("account not frozen"));
	}

	@Transactional
	public AccountEntity unFreezeAccountForUser(
			@NonNull UserLogin userLogin,
			@NonNull AccountNumber accountNumber
	) {
		UserEntity user = userService.findNotBlockedUserByLogin(userLogin);

		AccountEntity account = findAccountOrThrow(accountNumber);

		if (!account.userId().equals(user.id())) {
			throw new BusinessLogicException("account does not belong to this user");
		}

		if (account.status().equals(AccountEntity.Status.ACTIVE)) {
			return account;
		} else if (!account.status().equals(AccountEntity.Status.FROZEN)) {
			throw new BusinessLogicException("account is not frozen");
		}

		return accountRepository
				.changeAccountStatus(accountNumber, AccountEntity.Status.ACTIVE)
				.orElseThrow(() -> new BusinessLogicException("account not unfrozen"));
	}

	@Transactional
	public AccountEntity blockAccountByNumber(
			@NonNull UserLogin managerLogin,
			@NonNull AccountNumber accountNumber
	) {
		userService.findNotBlockedUserByLogin(managerLogin);

		AccountEntity account = findAccountOrThrow(accountNumber);
		if (account.status().equals(AccountEntity.Status.BLOCKED)) {
			return account;
		} else if (account.status().equals(AccountEntity.Status.CLOSED)) {
			throw new BusinessLogicException("closed account cannot be blocked");
		}

		return accountRepository
				.changeAccountStatus(accountNumber, AccountEntity.Status.BLOCKED)
				.orElseThrow(() -> new BusinessLogicException("account not blocked"));
	}

	@Transactional
	public AccountEntity unBlockAccountByNumber(
			@NonNull UserLogin managerLogin,
			@NonNull AccountNumber accountNumber
	) {
		userService.findNotBlockedUserByLogin(managerLogin);

		AccountEntity account = findAccountOrThrow(accountNumber);
		if (account.status().equals(AccountEntity.Status.CLOSED)) {
			throw new BusinessLogicException("closed account cannot be unblocked");
		} else if (!account.status().equals(AccountEntity.Status.BLOCKED)) {
			return account;
		}

		return accountRepository
				.changeAccountStatus(accountNumber, AccountEntity.Status.ACTIVE)
				.orElseThrow(() -> new BusinessLogicException("account not unblocked"));
	}

	public AccountEntity findAccountOrThrow(@NonNull Long accountId) {
		return accountRepository
				.findAccountById(accountId)
				.orElseThrow(() -> new ResourceNotFoundException("account not found"));
	}

	private AccountEntity findAccountOrThrow(@NonNull AccountNumber accountNumber) {
		return accountRepository
				.findByNumber(accountNumber)
				.orElseThrow(() -> new ResourceNotFoundException("account not found"));
	}

	private AccountEntity changeBalanceOrThrow(@NonNull AccountNumber accountNumber, @NonNull BigDecimal delta) {
		return accountRepository
				.changeAccountBalance(accountNumber, delta)
				.orElseThrow(() -> diagnoseBalanceChangeFailure(accountNumber, delta));
	}

	private RuntimeException diagnoseBalanceChangeFailure(
			@NonNull AccountNumber accountNumber,
			@NonNull BigDecimal delta
	) {
		AccountEntity account = findAccountOrThrow(accountNumber);

		if (!AccountEntity.Status.ACTIVE.equals(account.status()))
			throw new BusinessLogicException("account is inactive");

		if (account.balance().add(delta).compareTo(BigDecimal.ZERO) < 0)
			throw new BusinessLogicException("insufficient funds");

		throw new BusinessLogicException("unknown exception");
	}

	private void validateAmount(@NonNull BigDecimal amount) {
		if (amount.signum() <= 0)
			throw new BusinessLogicException("amount must be greater than 0");
	}
}
