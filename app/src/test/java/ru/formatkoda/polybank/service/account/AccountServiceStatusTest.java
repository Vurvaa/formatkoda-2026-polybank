package ru.formatkoda.polybank.service.account;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
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

@ExtendWith(MockitoExtension.class)
class AccountServiceStatusTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldReturnCorrectAccountDetails() {
        String testLogin = "TestLogin";
        Long testUserId = 1L;
        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findUserByLogin(new UserLogin(testLogin))).thenReturn(userEntity);

        AccountEntity accountEntity = mock(AccountEntity.class);
        when(accountEntity.userId()).thenReturn(testUserId);

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        AccountEntity checkAccountEntity = accountService.findOwnedAccount(
                new AccountNumber(testAccountNumber),
                userLogin
        );

        Assertions.assertEquals(accountEntity, checkAccountEntity);
        verify(userService, times(1)).findUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(
                        new AccountNumber(testAccountNumber)
                );

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldNotReturnSomeoneElseAccountDetails() {
        String testLogin = "TestLogin";

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        Long testUserId = 1L;
        Long testAccountOwnerId = 2L;

        Assertions.assertNotEquals(testAccountOwnerId, testUserId);

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = mock(AccountEntity.class);
        when(accountEntity.userId()).thenReturn(testAccountOwnerId);

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));

        AccountNumber testAccountNumberInstance = new AccountNumber(testAccountNumber);

        Assertions.assertThrows(
                BusinessLogicException.class,
                () -> accountService.findOwnedAccount(
                        testAccountNumberInstance,
                        userLogin
                )
        );

        verify(userService, times(1)).findUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
    }

    @Test
    void shouldNotReturnUnexistingAccountDetails() {
        String testLogin = "TestLogin";

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";

        UserEntity userEntity = mock(UserEntity.class);
        when(userService.findUserByLogin(userLogin)).thenReturn(userEntity);

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.empty());

        AccountNumber testAccountNumberInstance = new AccountNumber(testAccountNumber);

        Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.findOwnedAccount(
                        testAccountNumberInstance,
                        userLogin
                )
        );

        verify(userService, times(1)).findUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(
                        new AccountNumber(testAccountNumber)
                );


        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(AccountEntity.Type.class)
    void shouldCloseNotZeroBalanceAccount(AccountEntity.Type type) {
        String testLogin = "TestLogin";

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        Long testUserId = 1L;
        Long testAccountId = 1L;

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ZERO,
                type,
                AccountEntity.Status.ACTIVE,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );
        AccountEntity accountEntityClosed = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ZERO,
                type,
                AccountEntity.Status.CLOSED,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));
        when(accountRepository
                .changeAccountStatus(
                        accountNumber,
                        AccountEntity.Status.CLOSED
                )
        ).thenReturn(Optional.of(accountEntityClosed));

        AccountEntity accountEntityToCheck = accountService.closeAccountForUser(
                userLogin,
                accountNumber
        );

        Assertions.assertEquals(accountEntityClosed, accountEntityToCheck);
        Assertions.assertEquals(AccountEntity.Status.CLOSED, accountEntityToCheck.status());

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(1)).changeAccountStatus(accountNumber, AccountEntity.Status.CLOSED);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(AccountEntity.Type.class)
    void shouldNotCloseAlreadyClosedAccount(AccountEntity.Type type) {
        String testLogin = "TestLogin";

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        Long testUserId = 1L;
        Long testAccountId = 1L;

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ZERO,
                type,
                AccountEntity.Status.CLOSED,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));

        AccountEntity accountEntityToCheck = accountService.closeAccountForUser(
                userLogin,
                accountNumber
        );

        Assertions.assertEquals(accountEntity, accountEntityToCheck);
        Assertions.assertEquals(AccountEntity.Status.CLOSED, accountEntityToCheck.status());

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.CLOSED);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(AccountEntity.Type.class)
    void shouldNotCloseNotZeroBalanceAccount(AccountEntity.Type type) {
        String testLogin = "TestLogin";

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        Long testUserId = 1L;
        Long testAccountId = 1L;

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ONE,
                type,
                AccountEntity.Status.CLOSED,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));

        Assertions.assertThrows(
                BusinessLogicException.class,
                () -> accountService.closeAccountForUser(
                        userLogin,
                        accountNumber
                )
        );

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.CLOSED);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(AccountEntity.Type.class)
    void shouldNotCloseAnotherUserAccount(AccountEntity.Type type) {
        String testLogin = "TestLogin";
        Long testUserId = 1L;

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        Long testAccountId = 1L;
        Long testAccountOwnerId = 2L;

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = new AccountEntity(
                testAccountId,
                accountNumber,
                testAccountOwnerId,
                BigDecimal.ONE,
                type,
                AccountEntity.Status.CLOSED,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));

        Assertions.assertThrows(
                BusinessLogicException.class,
                () -> accountService.closeAccountForUser(
                        userLogin,
                        accountNumber
                )
        );

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.CLOSED);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldNotCloseAccountByBlockedUser() {
        String testLogin = "TestLogin";

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        when(userService.findNotBlockedUserByLogin(userLogin))
                .thenThrow(new BusinessLogicException("user is blocked"));

        Assertions.assertThrows(
                BusinessLogicException.class,
                () -> accountService.closeAccountForUser(
                        userLogin,
                        accountNumber
                )
        );

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(0))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.CLOSED);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccountEntity.Status.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"ACTIVE", "CLOSED"}
    )
    void shouldNotCloseNotActiveOrClosedAccount(AccountEntity.Status status) {
        String testLogin = "TestLogin";

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        Long testUserId = 1L;
        Long testAccountId = 1L;

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ZERO,
                AccountEntity.Type.CURRENT,
                status,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));

        Assertions.assertThrows(
                BusinessLogicException.class,
                () -> accountService.closeAccountForUser(
                        userLogin,
                        accountNumber
                )
        );

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.CLOSED);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccountEntity.Type.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = "CREDIT"
    )
    void shouldFreezeActiveNotCreditAccount(AccountEntity.Type type) {
        String testLogin = "TestLogin";

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        Long testUserId = 1L;
        Long testAccountId = 1L;

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ONE,
                type,
                AccountEntity.Status.ACTIVE,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );
        AccountEntity accountEntityFrozen = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ONE,
                type,
                AccountEntity.Status.FROZEN,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));
        when(accountRepository
                .changeAccountStatus(
                        accountNumber,
                        AccountEntity.Status.FROZEN
                )
        ).thenReturn(Optional.of(accountEntityFrozen));

        AccountEntity accountEntityToCheck = accountService.freezeAccountForUser(
                userLogin,
                accountNumber
        );

        Assertions.assertEquals(accountEntityFrozen, accountEntityToCheck);
        Assertions.assertEquals(AccountEntity.Status.FROZEN, accountEntityToCheck.status());

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(1)).changeAccountStatus(accountNumber, AccountEntity.Status.FROZEN);

        verifyNoMoreInteractions(userService, accountRepository);
    }


    @ParameterizedTest
    @EnumSource(
            value = AccountEntity.Type.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = "CREDIT"
    )
    void shouldNotFreezeAlreadyFrozenAccount(AccountEntity.Type type) {
        String testLogin = "TestLogin";

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        Long testUserId = 1L;
        Long testAccountId = 1L;

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ONE,
                type,
                AccountEntity.Status.FROZEN,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));

        AccountEntity accountEntityToCheck = accountService.freezeAccountForUser(
                userLogin,
                accountNumber
        );

        Assertions.assertEquals(accountEntity, accountEntityToCheck);
        Assertions.assertEquals(AccountEntity.Status.FROZEN, accountEntityToCheck.status());

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.FROZEN);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccountEntity.Type.class,
            mode = EnumSource.Mode.INCLUDE,
            names = "CREDIT"
    )
    void shouldNotCloseCreditAccount(AccountEntity.Type type) {
        String testLogin = "TestLogin";
        Long testUserId = 1L;
        Long testAccountId = 1L;

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ONE,
                type,
                AccountEntity.Status.CLOSED,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));

        Assertions.assertThrows(
                BusinessLogicException.class,
                () -> accountService.freezeAccountForUser(
                        userLogin,
                        accountNumber
                )
        );

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.FROZEN);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccountEntity.Type.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = "CREDIT"
    )
    void shouldNotFreezeAnotherUserAccount(AccountEntity.Type type) {
        String testLogin = "TestLogin";
        Long testUserId = 1L;
        Long testAccountId = 1L;
        Long testAccountOwnerId = 2L;

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = new AccountEntity(
                testAccountId,
                accountNumber,
                testAccountOwnerId,
                BigDecimal.ONE,
                type,
                AccountEntity.Status.FROZEN,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));

        Assertions.assertThrows(
                BusinessLogicException.class,
                () -> accountService.freezeAccountForUser(
                        userLogin,
                        accountNumber
                )
        );

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.FROZEN);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldNotFreezeAccountByBlockedUser() {
        String testLogin = "TestLogin";

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        when(userService.findNotBlockedUserByLogin(userLogin))
                .thenThrow(new BusinessLogicException("user is blocked"));

        Assertions.assertThrows(
                BusinessLogicException.class,
                () -> accountService.freezeAccountForUser(
                        userLogin,
                        accountNumber
                )
        );

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(0))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.FROZEN);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccountEntity.Status.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"ACTIVE", "FROZEN"}
    )
    void shouldNotFreezeNotActiveOrFrozenAccount(AccountEntity.Status status) {
        String testLogin = "TestLogin";
        Long testUserId = 1L;
        Long testAccountId = 1L;

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ONE,
                AccountEntity.Type.CURRENT,
                status,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));

        Assertions.assertThrows(
                BusinessLogicException.class,
                () -> accountService.freezeAccountForUser(
                        userLogin,
                        accountNumber
                )
        );

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.FROZEN);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccountEntity.Type.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = "CREDIT"
    )
    void shouldUnfreezeFrozenAccount(AccountEntity.Type type) {
        String testLogin = "TestLogin";
        Long testUserId = 1L;
        Long testAccountId = 1L;

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntity = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ONE,
                type,
                AccountEntity.Status.FROZEN,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );
        AccountEntity accountEntityUnFrozen = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ONE,
                type,
                AccountEntity.Status.ACTIVE,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntity));
        when(accountRepository
                .changeAccountStatus(
                        accountNumber,
                        AccountEntity.Status.ACTIVE
                )
        ).thenReturn(Optional.of(accountEntityUnFrozen));

        AccountEntity accountEntityToCheck = accountService.unFreezeAccountForUser(
                userLogin,
                accountNumber
        );

        Assertions.assertEquals(accountEntityUnFrozen, accountEntityToCheck);
        Assertions.assertEquals(AccountEntity.Status.ACTIVE, accountEntityToCheck.status());

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(1)).changeAccountStatus(accountNumber, AccountEntity.Status.ACTIVE);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldNotUnfreezeAccountByBlockedUser() {
        String testLogin = "TestLogin";

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        when(userService.findNotBlockedUserByLogin(userLogin))
                .thenThrow(new BusinessLogicException("user is blocked"));

        Assertions.assertThrows(
                BusinessLogicException.class,
                () -> accountService.unFreezeAccountForUser(
                        userLogin,
                        accountNumber
                )
        );

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(0))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.ACTIVE);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccountEntity.Type.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = "CREDIT"
    )
    void shouldNotUnfreezeActiveAccount(AccountEntity.Type type) {
        String testLogin = "TestLogin";
        Long testUserId = 1L;
        Long testAccountId = 1L;

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntityUnFrozen = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ONE,
                type,
                AccountEntity.Status.ACTIVE,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntityUnFrozen));

        AccountEntity accountEntityToCheck = accountService.unFreezeAccountForUser(
                userLogin,
                accountNumber
        );

        Assertions.assertEquals(accountEntityUnFrozen, accountEntityToCheck);
        Assertions.assertEquals(AccountEntity.Status.ACTIVE, accountEntityToCheck.status());

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.ACTIVE);

        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccountEntity.Status.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"ACTIVE", "FROZEN"}
    )
    void shouldNotUnfreezeActiveAccount(AccountEntity.Status status) {
        String testLogin = "TestLogin";
        Long testUserId = 1L;
        Long testAccountId = 1L;

        UserLogin userLogin = new UserLogin(
                testLogin
        );

        String testAccountNumber = AccountEntity.ACCOUNT_NUMBER_PREFIX + "1234123412341234";
        AccountNumber accountNumber = new AccountNumber(testAccountNumber);

        UserEntity userEntity = mock(UserEntity.class);
        when(userEntity.id()).thenReturn(testUserId);
        when(userService.findNotBlockedUserByLogin(userLogin)).thenReturn(userEntity);

        AccountEntity accountEntityUnFrozen = new AccountEntity(
                testAccountId,
                accountNumber,
                testUserId,
                BigDecimal.ONE,
                AccountEntity.Type.SAVINGS,
                status,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")
        );

        when(accountRepository
                .findByNumber(new AccountNumber(testAccountNumber))
        ).thenReturn(Optional.of(accountEntityUnFrozen));

        Assertions.assertThrows(
                BusinessLogicException.class,
                () -> accountService.unFreezeAccountForUser(
                        userLogin,
                        accountNumber
                )
        );

        verify(userService, times(1)).findNotBlockedUserByLogin(userLogin);
        verify(accountRepository, times(1))
                .findByNumber(new AccountNumber(testAccountNumber));
        verify(accountRepository, times(0)).changeAccountStatus(accountNumber, AccountEntity.Status.ACTIVE);

        verifyNoMoreInteractions(userService, accountRepository);
    }
}
