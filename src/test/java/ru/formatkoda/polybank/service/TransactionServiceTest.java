package ru.formatkoda.polybank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.domain.transaction.exception.InvalidTransactionException;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.repository.TransactionRepository;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
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
	private AccountService accountService;
	@InjectMocks
	private TransactionService transactionService;

	@ParameterizedTest
	@ValueSource(ints = {0, 5})
	void shouldReturnTransactionsWhenAccountBelongsToUser(int size) {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("67675678901234567890");
		PageRequest pageRequest = new PageRequest(0, 20);

		AccountEntity account = new AccountEntity(10L, new AccountNumber("67675678901234567890"));

		PageResult<TransactionWithAccountNumbersView> expectedResult = new PageResult<>(
				buildTransactionWithAccountNumbersViews(size),
				0,
				20,
				size
		);

		when(transactionRepository.findViewsByAccountId(account.id(), pageRequest)).thenReturn(expectedResult);

		when(accountService.findOwnedAccount(accountNumber, userLogin)).thenReturn(account);

		PageResult<TransactionWithAccountNumbersView> actualResult =
				transactionService.findByAccountNumber(
						accountNumber,
						userLogin,
						pageRequest
				);

		assertSame(expectedResult, actualResult);

		verify(accountService).findOwnedAccount(accountNumber, userLogin);
		verify(transactionRepository).findViewsByAccountId(account.id(), pageRequest);
	}

	@Test
	void topUpShouldIncreaseBalanceCreateDepositTransactionAndReturnView() {
		AccountNumber accountNumber = new AccountNumber("12345678901234567890");
		UserLogin userLogin = new UserLogin("user");
		BigDecimal amount = new BigDecimal("100.50");

		AccountEntity account = new AccountEntity(
				1L,
				accountNumber,
				10L,
				new BigDecimal("300.50")
		);

		OffsetDateTime createdAt = OffsetDateTime.of(2026, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC);

		TransactionEntity savedTransaction = new TransactionEntity(
				100L,
				null,
				1L,
				amount,
				TransactionEntity.Type.DEPOSIT,
				TransactionEntity.Status.COMPLETED,
				createdAt
		);

		when(accountRepository.increaseBalanceByNumberAndUserLoginIfAccountIsActive(
				accountNumber,
				userLogin,
				amount
		)).thenReturn(Optional.of(account));

		when(transactionRepository.save(ArgumentMatchers.any(TransactionEntity.class)))
				.thenReturn(savedTransaction);

		TransactionWithAccountNumbersView result = transactionService.topUp(
				accountNumber,
				amount,
				userLogin
		);

		assertThat(result.id()).isEqualTo(100L);
		assertThat(result.fromAccountNumber()).isNull();
		assertThat(result.toAccountNumber()).isEqualTo(accountNumber);
		assertThat(result.amount()).isEqualByComparingTo(amount);
		assertThat(result.type()).isEqualTo(TransactionEntity.Type.DEPOSIT);
		assertThat(result.status()).isEqualTo(TransactionEntity.Status.COMPLETED);
		assertThat(result.createdAt()).isEqualTo(createdAt);

		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);

		verify(transactionRepository).save(transactionCaptor.capture());

		TransactionEntity transactionToSave = transactionCaptor.getValue();

		assertThat(transactionToSave.id()).isNull();
		assertThat(transactionToSave.fromAccountId()).isNull();
		assertThat(transactionToSave.toAccountId()).isEqualTo(1L);
		assertThat(transactionToSave.amount()).isEqualByComparingTo(amount);
		assertThat(transactionToSave.type()).isEqualTo(TransactionEntity.Type.DEPOSIT);
		assertThat(transactionToSave.status()).isEqualTo(TransactionEntity.Status.COMPLETED);
		assertThat(transactionToSave.createdAt()).isNotNull();

		verify(accountRepository).increaseBalanceByNumberAndUserLoginIfAccountIsActive(
				accountNumber,
				userLogin,
				amount
		);
	}

	@ParameterizedTest
	@ValueSource(strings = {"0.0", "-1.0"})
	void topUpShouldRejectInvalidAmount(String amountValue) {
		AccountNumber accountNumber = new AccountNumber("12345678901234567890");
		UserLogin userLogin = new UserLogin("user");
		BigDecimal amount = new BigDecimal(amountValue);

		assertThatThrownBy(() -> transactionService.topUp(accountNumber, amount, userLogin))
				.isInstanceOf(InvalidTransactionException.class)
				.hasMessage("amount must be greater than 0");

		verifyNoInteractions(accountRepository);
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void topUpShouldRejectInactiveAccount() {
		AccountNumber accountNumber = new AccountNumber("12345678901234567890");
		UserLogin userLogin = new UserLogin("user");
		BigDecimal amount = new BigDecimal("100.00");

		when(accountRepository.increaseBalanceByNumberAndUserLoginIfAccountIsActive(
				accountNumber,
				userLogin,
				amount
		)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.topUp(accountNumber, amount, userLogin))
				.isInstanceOf(InvalidTransactionException.class)
				.hasMessage("account is not available for top up");

		verify(transactionRepository, never()).save(ArgumentMatchers.any());
	}

	@Test
	void topUpShouldNotCreateSuccessfulTransactionWhenAccountIsUnavailable() {
		AccountNumber accountNumber = new AccountNumber("12345678901234567890");
		UserLogin userLogin = new UserLogin("user");
		BigDecimal amount = new BigDecimal("100.00");

		when(accountRepository.increaseBalanceByNumberAndUserLoginIfAccountIsActive(
				accountNumber,
				userLogin,
				amount
		)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.topUp(accountNumber, amount, userLogin))
				.isInstanceOf(InvalidTransactionException.class);

		verify(transactionRepository, never()).save(ArgumentMatchers.any());
	}

	@Test
	void topUpShouldPropagateExceptionWhenTransactionCreationFails() {
		AccountNumber accountNumber = new AccountNumber("12345678901234567890");
		UserLogin userLogin = new UserLogin("user");
		BigDecimal amount = new BigDecimal("100.00");

		AccountEntity account = new AccountEntity(
				1L,
				accountNumber,
				10L,
				new BigDecimal("300.00")
		);

		when(accountRepository.increaseBalanceByNumberAndUserLoginIfAccountIsActive(
				accountNumber,
				userLogin,
				amount
		)).thenReturn(Optional.of(account));

		when(transactionRepository.save(ArgumentMatchers.any(TransactionEntity.class)))
				.thenThrow(new RuntimeException("transaction insert failed"));

		assertThatThrownBy(() -> transactionService.topUp(accountNumber, amount, userLogin))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("transaction insert failed");

		verify(accountRepository).increaseBalanceByNumberAndUserLoginIfAccountIsActive(
				accountNumber,
				userLogin,
				amount
		);
		verify(transactionRepository).save(org.mockito.ArgumentMatchers.any(TransactionEntity.class));
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
