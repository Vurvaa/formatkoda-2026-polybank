package ru.formatkoda.polybank.service.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.service.UserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.formatkoda.polybank.testutil.TestData.ACCOUNT_NUMBER;
import static ru.formatkoda.polybank.testutil.TestData.USER_LOGIN;
import static ru.formatkoda.polybank.testutil.TestData.account;
import static ru.formatkoda.polybank.testutil.TestData.user;

@ExtendWith(MockitoExtension.class)
class AccountServiceFindOwnedAccountTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private UserService userService;

	@InjectMocks
	private AccountService accountService;

	@Test
	void shouldReturnOwnedAccountWhenAccountBelongsToUser() {
		AccountEntity account = account();

		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

		AccountEntity actualAccount = accountService.findOwnedAccount(ACCOUNT_NUMBER, USER_LOGIN);

		assertThat(actualAccount).isSameAs(account);

		verify(userService).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
	}

	@Test
	void shouldThrowBusinessLogicExceptionWhenAccountDoesNotBelongToUser() {
		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account(3L)));

		assertThatThrownBy(() -> accountService.findOwnedAccount(ACCOUNT_NUMBER, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account does not belong to user");

		verify(userService).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
	}

	@Test
	void shouldThrowBusinessLogicExceptionWhenAccountNotFound() {
		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.findOwnedAccount(ACCOUNT_NUMBER, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account not found");

		verify(userService).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
	}

	@Test
	void shouldThrowBusinessLogicExceptionWhenUserNotFound() {
		when(userService.findUserByLogin(USER_LOGIN))
				.thenThrow(new BusinessLogicException("not found user with this login"));

		assertThatThrownBy(() -> accountService.findOwnedAccount(ACCOUNT_NUMBER, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("not found user with this login");

		verify(userService).findUserByLogin(USER_LOGIN);
		verifyNoInteractions(accountRepository);
	}
}
