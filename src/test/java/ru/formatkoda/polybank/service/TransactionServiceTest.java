package ru.formatkoda.polybank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.account.exception.AccountDoesNotBelongToCurrentUserException;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.exception.UserNotFoundException;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.repository.TransactionRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private AccountRepository accountRepository;
	@Mock
	private UserRepository userRepository;
	@InjectMocks
	private TransactionService transactionService;

	@ParameterizedTest
	@ValueSource(ints = {0, 5})
	void shouldReturnTransactionsWhenAccountBelongsToUser(int size) {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("67675678901234567890");
		PageRequest pageRequest = new PageRequest(0, 20);

		UserEntity user = buildUser();
		AccountEntity account = buildAccount();

		PageResult<TransactionWithAccountNumbersView> expectedResult = new PageResult<>(
				buildTransactionWithAccountNumbersViews(size),
				0,
				20,
				size
		);

		when(userRepository.findUserByLogin(userLogin.value())).thenReturn(Optional.of(user));

		when(accountRepository.findByNumberAndUserId(accountNumber, user.id())).thenReturn(Optional.of(account));

		when(transactionRepository.findViewsByAccountId(account.id(), pageRequest)).thenReturn(expectedResult);

		PageResult<TransactionWithAccountNumbersView> actualResult =
				transactionService.findByAccountNumber(
						accountNumber,
						userLogin,
						pageRequest
				);

		assertSame(expectedResult, actualResult);

		verify(userRepository).findUserByLogin(userLogin.value());
		verify(accountRepository).findByNumberAndUserId(accountNumber, user.id());
		verify(transactionRepository).findViewsByAccountId(account.id(), pageRequest);
	}

	@Test
	void shouldThrowAccountDoesNotBelongToCurrentUserExceptionWhenAccountDoesNotBelongToUser() {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("67675678901234567890");
		PageRequest pageRequest = new PageRequest(0, 20);

		UserEntity user = buildUser();

		when(userRepository.findUserByLogin(userLogin.value())).thenReturn(Optional.of(user));

		when(accountRepository.findByNumberAndUserId(accountNumber, user.id())).thenReturn(Optional.empty());

		assertThrows(
				AccountDoesNotBelongToCurrentUserException.class,
				() -> transactionService.findByAccountNumber(
						accountNumber,
						userLogin,
						pageRequest
				)
		);

		verify(userRepository).findUserByLogin(userLogin.value());
		verify(accountRepository).findByNumberAndUserId(accountNumber, user.id());
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void shouldThrowUserNotFoundExceptionWhenUserNotFound() {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("67675678901234567890");
		PageRequest pageRequest = new PageRequest(0, 20);

		when(userRepository.findUserByLogin(userLogin.value())).thenReturn(Optional.empty());

		assertThrows(
				UserNotFoundException.class,
				() -> transactionService.findByAccountNumber(
						accountNumber,
						userLogin,
						pageRequest
				)
		);

		verify(userRepository).findUserByLogin(userLogin.value());
		verifyNoInteractions(accountRepository);
		verifyNoInteractions(transactionRepository);
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

	private AccountEntity buildAccount() {
		return new AccountEntity(10L, new AccountNumber("67675678901234567890"));
	}

	private List<TransactionWithAccountNumbersView> buildTransactionWithAccountNumbersViews(int size) {
		List<TransactionWithAccountNumbersView> transactions = new ArrayList<>();

		for (int i = 0; i < size; i++) {
			transactions.add(
					new TransactionWithAccountNumbersView(
							(long) i,
							new AccountNumber("67675678901234567890"),
							new AccountNumber("67670000000000000000"),
							new BigDecimal("100.50"),
							TransactionEntity.Type.DEPOSIT,
							TransactionEntity.Status.COMPLETED,
							OffsetDateTime.of(2026, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC)
					)
			);
		}

		return transactions;
	}
}
