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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.formatkoda.polybank.testutil.TestData.ACCOUNT_NUMBER;
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

	private AccountEntity buildAccount(AccountNumber accountNumber, Long userId) {
		return new AccountEntity(
				10L,
				accountNumber,
				userId,
				new BigDecimal("300.00")
		);
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
