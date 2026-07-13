package ru.formatkoda.polybank.service.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.service.UserService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.formatkoda.polybank.testutil.TestData.USER_LOGIN;
import static ru.formatkoda.polybank.testutil.TestData.account;
import static ru.formatkoda.polybank.testutil.TestData.user;

@ExtendWith(MockitoExtension.class)
class AccountServiceFindAllForUserTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private UserService userService;

	@InjectMocks
	private AccountService accountService;

	@Test
	void shouldReturnUserAccounts() {
		AccountEntity firstAccount = account();
		AccountEntity secondAccount = new AccountEntity(
				2L,
				new AccountNumber("12345678901234567891"),
				user().id(),
				new BigDecimal("250.00"),
				AccountEntity.Type.SAVINGS,
				AccountEntity.Status.BLOCKED,
				OffsetDateTime.parse("2026-06-29T12:00:00Z")
		);

		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findAllByUserId(user().id()))
				.thenReturn(List.of(firstAccount, secondAccount));

		List<AccountEntity> result = accountService.findAllForUser(USER_LOGIN);

		assertThat(result).containsExactly(firstAccount, secondAccount);

		verify(userService).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findAllByUserId(user().id());
		verifyNoMoreInteractions(userService, accountRepository);
	}

	@Test
	void shouldReturnEmptyListWhenUserHasNoAccounts() {
		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findAllByUserId(user().id())).thenReturn(List.of());

		List<AccountEntity> result = accountService.findAllForUser(USER_LOGIN);

		assertThat(result).isEmpty();

		verify(userService).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findAllByUserId(user().id());
		verifyNoMoreInteractions(userService, accountRepository);
	}

	@Test
	void shouldRequestAccountsOnlyForResolvedUser() {
		long anotherUserId = 3L;

		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findAllByUserId(user().id())).thenReturn(List.of(account()));

		List<AccountEntity> result = accountService.findAllForUser(USER_LOGIN);

		assertThat(result).containsExactly(account());

		verify(userService).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findAllByUserId(user().id());
		verify(accountRepository, never()).findAllByUserId(anotherUserId);
		verifyNoMoreInteractions(userService, accountRepository);
	}

	@Test
	void shouldNotChangeAccountsOrTransactions() {
		when(userService.findUserByLogin(USER_LOGIN)).thenReturn(user());
		when(accountRepository.findAllByUserId(user().id())).thenReturn(List.of(account()));

		List<AccountEntity> result = accountService.findAllForUser(USER_LOGIN);

		assertThat(result).containsExactly(account());

		verify(userService).findUserByLogin(USER_LOGIN);
		verify(accountRepository).findAllByUserId(user().id());
		verify(accountRepository, never()).createAccountForUser(anyLong(), any());
		verify(accountRepository, never()).changeAccountBalance(any(), any());
		verify(accountRepository, never()).findByNumber(any());
		verifyNoMoreInteractions(userService, accountRepository);
	}
}
