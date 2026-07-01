package ru.formatkoda.polybank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountEntity.AccountType;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.exceptions.BusinessLogicException;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.security.UserSession;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;

import java.math.BigDecimal;
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
