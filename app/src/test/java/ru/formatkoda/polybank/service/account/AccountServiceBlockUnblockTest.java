package ru.formatkoda.polybank.service.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.service.UserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.formatkoda.polybank.testutil.TestData.ACCOUNT_NUMBER;
import static ru.formatkoda.polybank.testutil.TestData.AMOUNT;
import static ru.formatkoda.polybank.testutil.TestData.CREATED_AT;
import static ru.formatkoda.polybank.testutil.TestData.SENIOR_MANAGER_LOGIN;
import static ru.formatkoda.polybank.testutil.TestData.userManager;

@ExtendWith(MockitoExtension.class)
class AccountServiceBlockUnblockTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AccountService accountService;

    @ParameterizedTest
    @EnumSource(
            value = AccountEntity.Status.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"BLOCKED", "CLOSED"}
    )
    void shouldBlockAccountByNumber(AccountEntity.Status status) {
        AccountEntity account = accountWithStatus(status);
        AccountEntity blockedAccount = accountWithStatus(AccountEntity.Status.BLOCKED);

        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(accountRepository.changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED)).thenReturn(Optional.of(blockedAccount));

        AccountEntity result = accountService.blockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER);

        assertThat(result).isSameAs(blockedAccount);
        assertThat(result.status()).isEqualTo(AccountEntity.Status.BLOCKED);

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
        verify(accountRepository).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE);
        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldReturnAccountWhenBlockingAlreadyBlockedAccount() {
        AccountEntity blockedAccount = accountWithStatus(AccountEntity.Status.BLOCKED);

        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(blockedAccount));

        AccountEntity result = accountService.blockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER);

        assertThat(result).isSameAs(blockedAccount);
        assertThat(result.status()).isEqualTo(AccountEntity.Status.BLOCKED);

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE);
        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldNotBlockClosedAccount() {
        AccountEntity closedAccount = accountWithStatus(AccountEntity.Status.CLOSED);

        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(closedAccount));

        assertThatThrownBy(() -> accountService.blockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("closed account cannot be blocked");

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE);
        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldNotBlockMissingAccount() {
        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.blockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("account not found");

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE);
        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldNotBlockAccountWhenManagerIsBlocked() {
        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN))
                .thenThrow(new BusinessLogicException("manager is blocked"));

        assertThatThrownBy(() -> accountService.blockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("manager is blocked");

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verifyNoMoreInteractions(userService);
        verifyNoInteractions(accountRepository);
    }

    @Test
    void shouldThrowBusinessLogicExceptionWhenBlockUpdateFails() {
        AccountEntity account = accountWithStatus(AccountEntity.Status.ACTIVE);

        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(accountRepository.changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.blockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("account not blocked");

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
        verify(accountRepository).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE);
        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldUnblockBlockedAccountByNumber() {
        AccountEntity blockedAccount = accountWithStatus(AccountEntity.Status.BLOCKED);
        AccountEntity activeAccount = accountWithStatus(AccountEntity.Status.ACTIVE);

        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(blockedAccount));
        when(accountRepository.changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE)).thenReturn(Optional.of(activeAccount));

        AccountEntity result = accountService.unBlockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER);

        assertThat(result).isSameAs(activeAccount);
        assertThat(result.status()).isEqualTo(AccountEntity.Status.ACTIVE);

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
        verify(accountRepository).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED);
        verifyNoMoreInteractions(userService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccountEntity.Status.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"BLOCKED", "CLOSED"}
    )
    void shouldReturnAccountWhenUnblockingNotBlockedAccount(AccountEntity.Status status) {
        AccountEntity account = accountWithStatus(status);

        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        AccountEntity result = accountService.unBlockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER);

        assertThat(result).isSameAs(account);
        assertThat(result.status()).isEqualTo(status);

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED);
        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldNotUnblockClosedAccount() {
        AccountEntity closedAccount = accountWithStatus(AccountEntity.Status.CLOSED);

        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(closedAccount));

        assertThatThrownBy(() -> accountService.unBlockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER))
                .isInstanceOf(BusinessLogicException.class);

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED);
        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldNotUnblockMissingAccount() {
        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.unBlockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("account not found");

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED);
        verifyNoMoreInteractions(userService, accountRepository);
    }

    @Test
    void shouldNotUnblockAccountWhenManagerIsBlocked() {
        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN))
                .thenThrow(new BusinessLogicException("manager is blocked"));

        assertThatThrownBy(() -> accountService.unBlockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("manager is blocked");

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verifyNoMoreInteractions(userService);
        verifyNoInteractions(accountRepository);
    }

    @Test
    void shouldThrowBusinessLogicExceptionWhenUnblockUpdateFails() {
        AccountEntity blockedAccount = accountWithStatus(AccountEntity.Status.BLOCKED);

        when(userService.findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(blockedAccount));
        when(accountRepository.changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.unBlockAccountByNumber(SENIOR_MANAGER_LOGIN, ACCOUNT_NUMBER))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("account not unblocked");

        verify(userService).findNotBlockedUserByLogin(SENIOR_MANAGER_LOGIN);
        verify(accountRepository).findByNumber(ACCOUNT_NUMBER);
        verify(accountRepository).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.ACTIVE);
        verify(accountRepository, never()).changeAccountStatus(ACCOUNT_NUMBER, AccountEntity.Status.BLOCKED);
        verifyNoMoreInteractions(userService, accountRepository);
    }

    private AccountEntity accountWithStatus(AccountEntity.Status status) {
        return new AccountEntity(
                1L,
                ACCOUNT_NUMBER,
                10L,
                AMOUNT,
                AccountEntity.Type.CURRENT,
                status,
                CREATED_AT
        );
    }
}
