package ru.formatkoda.polybank.service.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountEntity.AccountType;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.security.UserSession;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.formatkoda.polybank.testutil.TestData.ACCOUNT_NUMBER;
import static ru.formatkoda.polybank.testutil.TestData.AMOUNT;
import static ru.formatkoda.polybank.testutil.TestData.USER_LOGIN;
import static ru.formatkoda.polybank.testutil.TestData.account;
import static ru.formatkoda.polybank.testutil.TestData.user;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AccountService accountService;

	@ParameterizedTest
	@EnumSource(AccountType.class)
	void shouldCreateAccount(AccountType accountType) {
		String testLogin = "TestLogin";

		UserEntity userEntity = mock(UserEntity.class);
		when(userEntity.id()).thenReturn(1L);

		when(userService.findUserByLogin(testLogin)).thenReturn(userEntity);

		AccountEntity accountEntity = new AccountEntity(
				0L,
				new AccountNumber("67671234123412341234"),
				1L,
				BigDecimal.ZERO,
				accountType,
				AccountEntity.AccountStatus.ACTIVE,
				OffsetDateTime.parse("2026-06-29T12:00:00Z")
		);
		when(accountRepository.createAccountForUser(userEntity.id(), accountType)).thenReturn(Optional.of(accountEntity));

		UserSession userSession = new UserSession(
				testLogin
		);

		AccountEntity accountEntityToCheck = accountService.createAccountForUser(userSession, accountType);

		Assertions.assertEquals(accountEntity, accountEntityToCheck);

		verify(userService, times(1)).findUserByLogin(testLogin);
		verify(accountRepository, times(1)).createAccountForUser(userEntity.id(), accountType);

		verifyNoMoreInteractions(userService, accountRepository);
	}

	@ParameterizedTest
	@EnumSource(AccountType.class)
	void shouldNotCreateAccountWhenUserIsBlocked(AccountType accountType) {
		String testLogin = "TestLogin";

		UserEntity userEntity = mock(UserEntity.class);
		when(userEntity.id()).thenReturn(1L);
		when(userEntity.isBlocked()).thenReturn(true);

		when(userService.findUserByLogin(testLogin)).thenReturn(userEntity);

		UserSession userSession = new UserSession(
				testLogin
		);

		Assertions.assertThrows(
				BusinessLogicException.class,
				() -> accountService.createAccountForUser(userSession, accountType)
		);

		verify(userService, times(1)).findUserByLogin(testLogin);
		verify(accountRepository, times(0))
				.createAccountForUser(userEntity.id(), accountType);

		verifyNoMoreInteractions(userService, accountRepository);
	}

	@Test
	void shouldReturnOwnedAccountWhenAccountBelongsToUser() {
		AccountEntity account = account();

		when(userRepository.findUserByLogin(USER_LOGIN)).thenReturn(Optional.of(user()));
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

		AccountEntity actualAccount = accountService.findOwnedAccount(ACCOUNT_NUMBER, USER_LOGIN);

		assertThat(actualAccount).isSameAs(account);

		verify(userRepository).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
	}

	@Test
	void shouldThrowBusinessLogicExceptionWhenAccountDoesNotBelongToUser() {
		when(userRepository.findUserByLogin(USER_LOGIN)).thenReturn(Optional.of(user()));
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account(3L)));

		assertThatThrownBy(() -> accountService.findOwnedAccount(ACCOUNT_NUMBER, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account does not belong to user");

		verify(userRepository).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
	}

	@Test
	void shouldThrowBusinessLogicExceptionWhenAccountNotFound() {
		when(userRepository.findUserByLogin(USER_LOGIN)).thenReturn(Optional.of(user()));
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.findOwnedAccount(ACCOUNT_NUMBER, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account not found");

		verify(userRepository).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
	}

	@Test
	void shouldThrowBusinessLogicExceptionWhenUserNotFound() {
		when(userRepository.findUserByLogin(USER_LOGIN)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.findOwnedAccount(ACCOUNT_NUMBER, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("user not found");

		verify(userRepository).findUserByLogin(USER_LOGIN);
		verifyNoInteractions(accountRepository);
	}

	@Test
	void topUpOwnedAccountShouldIncreaseBalanceThroughRepositoryAndReturnUpdatedAccount() {
		AccountEntity updatedAccount = account();

		when(userRepository.findUserByLogin(USER_LOGIN)).thenReturn(Optional.of(user()));
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account()));
		when(accountRepository.changeAccountBalance(ACCOUNT_NUMBER, AMOUNT)).thenReturn(Optional.of(updatedAccount));

		AccountEntity result = accountService.topUpOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN);

		assertThat(result).isSameAs(updatedAccount);

		verify(userRepository).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository).changeAccountBalance(ACCOUNT_NUMBER, AMOUNT);
	}

	@Test
	void withdrawFromOwnedAccountShouldDecreaseBalanceThroughRepositoryAndReturnUpdatedAccount() {
		AccountEntity updatedAccount = account();

		when(userRepository.findUserByLogin(USER_LOGIN)).thenReturn(Optional.of(user()));
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account()));
		when(accountRepository.changeAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate())).thenReturn(Optional.of(updatedAccount));

		AccountEntity result = accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN);

		assertThat(result).isSameAs(updatedAccount);

		verify(userRepository).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository).changeAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
	}

	@Test
	void topUpOwnedAccountShouldRejectZeroAmountAndNotReadOrChangeAccount() {
		BigDecimal amount = BigDecimal.ZERO;

		assertThatThrownBy(() -> accountService.topUpOwnedAccount(ACCOUNT_NUMBER, amount, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("amount must be greater than 0");

		verifyNoInteractions(userRepository);
		verifyNoInteractions(accountRepository);
	}

	@Test
	void withdrawFromOwnedAccountShouldRejectNegativeAmountAndNotReadOrChangeAccount() {
		BigDecimal amount = new BigDecimal("-1.00");

		assertThatThrownBy(() -> accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, amount, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("amount must be greater than 0");

		verifyNoInteractions(userRepository);
		verifyNoInteractions(accountRepository);
	}

	@Test
	void topUpOwnedAccountShouldNotChangeBalanceWhenAccountDoesNotBelongToUser() {
		when(userRepository.findUserByLogin(USER_LOGIN)).thenReturn(Optional.of(user()));
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account(3L)));

		assertThatThrownBy(() -> accountService.topUpOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account does not belong to user");

		verify(userRepository).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository, never()).changeAccountBalance(ACCOUNT_NUMBER, AMOUNT);
	}

	@Test
	void withdrawFromOwnedAccountShouldNotChangeBalanceWhenAccountDoesNotBelongToUser() {
		when(userRepository.findUserByLogin(USER_LOGIN)).thenReturn(Optional.of(user()));
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account(3L)));

		assertThatThrownBy(() -> accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account does not belong to user");

		verify(userRepository).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository, never()).changeAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
	}

	@Test
	void topUpOwnedAccountShouldDiagnoseAccountNotFoundWhenBalanceUpdateFails() {
		when(userRepository.findUserByLogin(USER_LOGIN)).thenReturn(Optional.of(user()));
		when(accountRepository.findByNumber(ACCOUNT_NUMBER))
				.thenReturn(Optional.of(account()))
				.thenReturn(Optional.empty());
		when(accountRepository.changeAccountBalance(ACCOUNT_NUMBER, AMOUNT)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.topUpOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account not found");

		verify(accountRepository, times(2)).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository).changeAccountBalance(ACCOUNT_NUMBER, AMOUNT);
	}

	@Test
	void withdrawFromOwnedAccountShouldDiagnoseInsufficientFundsWhenBalanceUpdateFails() {
		BigDecimal amount = new BigDecimal("101.00");

		when(userRepository.findUserByLogin(USER_LOGIN)).thenReturn(Optional.of(user()));
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account()));
		when(accountRepository.changeAccountBalance(ACCOUNT_NUMBER, amount.negate())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, amount, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("insufficient funds");

		verify(accountRepository, times(2)).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository).changeAccountBalance(ACCOUNT_NUMBER, amount.negate());
	}

	@Test
	void withdrawFromOwnedAccountShouldDiagnoseInactiveAccountWhenBalanceUpdateFails() {
		AccountEntity inactiveAccount = inactiveAccount();

		when(userRepository.findUserByLogin(USER_LOGIN)).thenReturn(Optional.of(user()));
		when(accountRepository.findByNumber(ACCOUNT_NUMBER))
				.thenReturn(Optional.of(account()))
				.thenReturn(Optional.of(inactiveAccount));
		when(accountRepository.changeAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account is inactive");

		verify(accountRepository, times(2)).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository).changeAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
	}

	private AccountEntity inactiveAccount() {
		AccountEntity account = mock(AccountEntity.class);

		when(account.status()).thenReturn(AccountEntity.Status.BLOCKED);

		return account;
	}
}
