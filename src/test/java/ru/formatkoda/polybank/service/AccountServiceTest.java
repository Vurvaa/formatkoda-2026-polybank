package ru.formatkoda.polybank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.account.exception.AccountDoesNotBelongToCurrentUserException;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.exception.UserNotFoundException;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.repository.UserRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
	@Mock
	private AccountRepository accountRepository;
	@Mock
	private UserRepository userRepository;
	@InjectMocks
	private AccountService accountService;

	@Test
	void shouldReturnOwnedAccountWhenAccountBelongsToUser() {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("67675678901234567890");

		UserEntity user = buildUser();
		AccountEntity account = new AccountEntity(10L, accountNumber);

		when(userRepository.findUserByLogin(userLogin.value())).thenReturn(Optional.of(user));
		when(accountRepository.findByNumberAndUserId(accountNumber, user.id())).thenReturn(Optional.of(account));

		AccountEntity actualAccount = accountService.findOwnedAccount(accountNumber, userLogin);

		assertThat(actualAccount).isSameAs(account);

		verify(userRepository).findUserByLogin(userLogin.value());
		verify(accountRepository).findByNumberAndUserId(accountNumber, user.id());
	}

	@Test
	void shouldThrowAccountDoesNotBelongToCurrentUserExceptionWhenAccountDoesNotBelongToUser() {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("67675678901234567890");

		UserEntity user = buildUser();

		when(userRepository.findUserByLogin(userLogin.value())).thenReturn(Optional.of(user));

		when(accountRepository.findByNumberAndUserId(accountNumber, user.id())).thenReturn(Optional.empty());

		assertThrows(
				AccountDoesNotBelongToCurrentUserException.class,
				() -> accountService.findOwnedAccount(
						accountNumber,
						userLogin
				)
		);

		verify(userRepository).findUserByLogin(userLogin.value());
		verify(accountRepository).findByNumberAndUserId(accountNumber, user.id());
	}

	@Test
	void shouldThrowUserNotFoundExceptionWhenUserNotFound() {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("67675678901234567890");

		when(userRepository.findUserByLogin(userLogin.value())).thenReturn(Optional.empty());

		assertThrows(
				UserNotFoundException.class,
				() -> accountService.findOwnedAccount(
						accountNumber,
						userLogin
				)
		);

		verify(userRepository).findUserByLogin(userLogin.value());
		verifyNoInteractions(accountRepository);
	}

	private UserEntity buildUser() {
		return new UserEntity(
				1L,
				"user@example.com",
				"User",
				"Test",
				"password-hash",
				OffsetDateTime.of(2026, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC),
				null
		);
	}
}
