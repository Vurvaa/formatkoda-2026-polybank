package ru.formatkoda.polybank.service.account;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountEntity.Type;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.messaging.publisher.AccountEventPublisher;
import ru.formatkoda.polybank.messaging.publisher.NotificationEventPublisher;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.service.UserService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.formatkoda.polybank.testutil.TestData.USER_EMAIL;
import static ru.formatkoda.polybank.testutil.TestData.USER_LOGIN;

@ExtendWith(MockitoExtension.class)
class AccountServiceCreateTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private UserService userService;

	@Mock
	private AccountEventPublisher accountEventPublisher;

	@Mock
	private NotificationEventPublisher notificationEventPublisher;

	@InjectMocks
	private AccountService accountService;

	@ParameterizedTest
	@EnumSource(Type.class)
	void shouldCreateAccount(Type accountType) {
		UserEntity userEntity = mock(UserEntity.class);
		when(userEntity.id()).thenReturn(1L);
		when(userEntity.name()).thenReturn("User");
		when(userEntity.email()).thenReturn(USER_EMAIL);

		when(userService.findNotBlockedUserByLogin(USER_LOGIN)).thenReturn(userEntity);

		AccountEntity accountEntity = new AccountEntity(
				0L,
				new AccountNumber("67671234123412341234"),
				1L,
				BigDecimal.ZERO,
				accountType,
				AccountEntity.Status.ACTIVE,
				OffsetDateTime.parse("2026-06-29T12:00:00Z")
		);
		when(accountRepository.createAccountForUser(userEntity.id(), accountType)).thenReturn(Optional.of(accountEntity));

		AccountEntity accountEntityToCheck = accountService.createAccountForUser(accountType, USER_LOGIN);

		Assertions.assertEquals(accountEntity, accountEntityToCheck);

		verify(userService, times(1)).findNotBlockedUserByLogin(USER_LOGIN);
		verify(accountEventPublisher).publishAccountCreated(accountEntity);
		verify(accountRepository, times(1)).createAccountForUser(userEntity.id(), accountType);

		verifyNoMoreInteractions(userService, accountRepository);
	}

	@ParameterizedTest
	@EnumSource(Type.class)
	void shouldNotCreateAccountWhenUserIsBlocked(Type accountType) {
		UserEntity userEntity = mock(UserEntity.class);
		when(userEntity.id()).thenReturn(1L);

		when(userService.findNotBlockedUserByLogin(USER_LOGIN))
				.thenThrow(new BusinessLogicException("user is blocked"));

		Assertions.assertThrows(
				BusinessLogicException.class,
				() -> accountService.createAccountForUser(accountType, USER_LOGIN)
		);

		verify(userService, times(1)).findNotBlockedUserByLogin(USER_LOGIN);
		verify(accountRepository, times(0))
				.createAccountForUser(userEntity.id(), accountType);

		verifyNoMoreInteractions(userService, accountRepository);
	}
}
