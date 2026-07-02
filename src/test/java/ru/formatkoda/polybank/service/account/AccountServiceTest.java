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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
		AccountEntity account = buildAccount(accountNumber, user.id());

		when(userRepository.findUserByLogin(userLogin.value())).thenReturn(Optional.of(user));
		when(accountRepository.findByNumber(accountNumber)).thenReturn(Optional.of(account));

		AccountEntity actualAccount = accountService.findOwnedAccount(accountNumber, userLogin);

		assertThat(actualAccount).isSameAs(account);

		verify(userRepository).findUserByLogin(userLogin.value());
		verify(accountRepository).findByNumber(accountNumber);
	}

	@Test
	void shouldThrowBusinessLogicExceptionWhenAccountDoesNotBelongToUser() {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("67675678901234567890");

		UserEntity user = buildUser();
		AccountEntity account = buildAccount(accountNumber, 999L);

		when(userRepository.findUserByLogin(userLogin.value())).thenReturn(Optional.of(user));
		when(accountRepository.findByNumber(accountNumber)).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> accountService.findOwnedAccount(accountNumber, userLogin))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account does not belong to user");

		verify(userRepository).findUserByLogin(userLogin.value());
		verify(accountRepository).findByNumber(accountNumber);
	}

	@Test
	void shouldThrowBusinessLogicExceptionWhenAccountNotFound() {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("67675678901234567890");

		UserEntity user = buildUser();

		when(userRepository.findUserByLogin(userLogin.value())).thenReturn(Optional.of(user));
		when(accountRepository.findByNumber(accountNumber)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.findOwnedAccount(accountNumber, userLogin))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account not found");

		verify(userRepository).findUserByLogin(userLogin.value());
		verify(accountRepository).findByNumber(accountNumber);
	}

	@Test
	void shouldThrowBusinessLogicExceptionWhenUserNotFound() {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("67675678901234567890");

		when(userRepository.findUserByLogin(userLogin.value())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.findOwnedAccount(accountNumber, userLogin))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("user not found");

		verify(userRepository).findUserByLogin(userLogin.value());
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
