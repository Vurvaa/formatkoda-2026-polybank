package ru.formatkoda.polybank.service.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.service.UserService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
class AccountServiceBalanceOperationTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AccountService accountService;

	@Test
	void topUpOwnedAccountShouldIncreaseBalanceThroughRepositoryAndReturnUpdatedAccount() {
		AccountEntity updatedAccount = account();

		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account()));
		when(accountRepository.changeAccountBalance(ACCOUNT_NUMBER, AMOUNT)).thenReturn(Optional.of(updatedAccount));

		AccountEntity result = accountService.topUpOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN);

		assertThat(result).isSameAs(updatedAccount);

		verify(userService).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository).changeAccountBalance(ACCOUNT_NUMBER, AMOUNT);
	}

	@Test
	void withdrawFromOwnedAccountShouldDecreaseBalanceThroughRepositoryAndReturnUpdatedAccount() {
		AccountEntity updatedAccount = account();

		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account()));
		when(accountRepository.changeAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate())).thenReturn(Optional.of(updatedAccount));

		AccountEntity result = accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN);

		assertThat(result).isSameAs(updatedAccount);

		verify(userService).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository).changeAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
	}

	@Test
	void topUpOwnedAccountShouldRejectZeroAmountAndNotReadOrChangeAccount() {
		BigDecimal amount = BigDecimal.ZERO;

		assertThatThrownBy(() -> accountService.topUpOwnedAccount(ACCOUNT_NUMBER, amount, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("amount must be greater than 0");

		verifyNoInteractions(userService, accountRepository);
	}

	@Test
	void withdrawFromOwnedAccountShouldRejectNegativeAmountAndNotReadOrChangeAccount() {
		BigDecimal amount = new BigDecimal("-1.00");

		assertThatThrownBy(() -> accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, amount, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("amount must be greater than 0");

		verifyNoInteractions(userService, accountRepository);
	}

	@Test
	void topUpOwnedAccountShouldNotChangeBalanceWhenAccountDoesNotBelongToUser() {
		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account(3L)));

		assertThatThrownBy(() -> accountService.topUpOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account does not belong to user");

		verify(userService).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository, never()).changeAccountBalance(ACCOUNT_NUMBER, AMOUNT);
	}

	@Test
	void withdrawFromOwnedAccountShouldNotChangeBalanceWhenAccountDoesNotBelongToUser() {
		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account(3L)));

		assertThatThrownBy(() -> accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account does not belong to user");

		verify(userService).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository, never()).changeAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
	}

	@Test
	void topUpOwnedAccountShouldDiagnoseAccountNotFoundWhenBalanceUpdateFails() {
		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findByNumber(ACCOUNT_NUMBER))
				.thenReturn(Optional.of(account()))
				.thenReturn(Optional.empty());
		when(accountRepository.changeAccountBalance(ACCOUNT_NUMBER, AMOUNT)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.topUpOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("account not found");

		verify(accountRepository, times(2)).findByNumber(ACCOUNT_NUMBER);
		verify(accountRepository).changeAccountBalance(ACCOUNT_NUMBER, AMOUNT);
	}

	@Test
	void withdrawFromOwnedAccountShouldDiagnoseInsufficientFundsWhenBalanceUpdateFails() {
		BigDecimal amount = new BigDecimal("101.00");

		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
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

		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
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
