package ru.formatkoda.polybank.service.transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.repository.TransactionRepository;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.service.TransactionService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.formatkoda.polybank.testutil.TransactionTestData.ACCOUNT_NUMBER;
import static ru.formatkoda.polybank.testutil.TransactionTestData.AMOUNT;
import static ru.formatkoda.polybank.testutil.TransactionTestData.CREATED_AT;
import static ru.formatkoda.polybank.testutil.TransactionTestData.USER_LOGIN;
import static ru.formatkoda.polybank.testutil.TransactionTestData.account;

@ExtendWith(MockitoExtension.class)
class TransactionServiceWithdrawTest {
	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private AccountService accountService;
	@InjectMocks
	private TransactionService transactionService;

	@Test
	void withdrawShouldDecreaseBalanceCreateWithdrawTransactionAndReturnView() {
		when(accountService.withdrawFromOwnedAccount(
				ACCOUNT_NUMBER,
				AMOUNT,
				USER_LOGIN
		)).thenReturn(account());

		when(transactionRepository.save(ArgumentMatchers.any(TransactionEntity.class)))
				.thenReturn(savedWithdrawalTransaction());

		TransactionWithAccountNumbersView result = transactionService.withdraw(
				ACCOUNT_NUMBER,
				AMOUNT,
				USER_LOGIN
		);

		assertThat(result.id()).isEqualTo(100L);
		assertThat(result.fromAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
		assertThat(result.toAccountNumber()).isNull();
		assertThat(result.amount()).isEqualByComparingTo(AMOUNT);
		assertThat(result.type()).isEqualTo(TransactionEntity.Type.WITHDRAWAL);
		assertThat(result.status()).isEqualTo(TransactionEntity.Status.COMPLETED);
		assertThat(result.createdAt()).isEqualTo(CREATED_AT);

		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);

		verify(transactionRepository).save(transactionCaptor.capture());

		TransactionEntity transactionToSave = transactionCaptor.getValue();

		assertThat(transactionToSave.id()).isNull();
		assertThat(transactionToSave.fromAccountId()).isEqualTo(1L);
		assertThat(transactionToSave.toAccountId()).isNull();
		assertThat(transactionToSave.amount()).isEqualByComparingTo(AMOUNT);
		assertThat(transactionToSave.type()).isEqualTo(TransactionEntity.Type.WITHDRAWAL);
		assertThat(transactionToSave.status()).isEqualTo(TransactionEntity.Status.COMPLETED);
		assertThat(transactionToSave.createdAt()).isNotNull();

		verify(accountService).withdrawFromOwnedAccount(
				ACCOUNT_NUMBER,
				AMOUNT,
				USER_LOGIN
		);
	}

	@ParameterizedTest
	@ValueSource(strings = {"0.0", "-1.0"})
	void withdrawShouldPropagateAccountServiceExceptionAndNotCreateTransaction(String amountValue) {
		BigDecimal amount = new BigDecimal(amountValue);

		when(accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, amount, USER_LOGIN))
				.thenThrow(new BusinessLogicException("amount must be greater than 0"));

		assertThatThrownBy(() -> transactionService.withdraw(ACCOUNT_NUMBER, amount, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("amount must be greater than 0");

		verify(accountService).withdrawFromOwnedAccount(ACCOUNT_NUMBER, amount, USER_LOGIN);
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void withdrawShouldPropagateAccountServiceExceptionWhenAccountCannotBeChanged() {
		when(accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.thenThrow(new BusinessLogicException("insufficient funds"));

		assertThatThrownBy(() -> transactionService.withdraw(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("insufficient funds");

		verify(accountService).withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN);
		verify(transactionRepository, never()).save(ArgumentMatchers.any());
	}

	@Test
	void withdrawShouldPropagateExceptionWhenTransactionCreationFails() {
		when(accountService.withdrawFromOwnedAccount(
				ACCOUNT_NUMBER,
				AMOUNT,
				USER_LOGIN
		)).thenReturn(account());

		when(transactionRepository.save(ArgumentMatchers.any(TransactionEntity.class)))
				.thenThrow(new RuntimeException("transaction insert failed"));

		assertThatThrownBy(() -> transactionService.withdraw(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("transaction insert failed");

		verify(accountService).withdrawFromOwnedAccount(
				ACCOUNT_NUMBER,
				AMOUNT,
				USER_LOGIN
		);
		verify(transactionRepository).save(ArgumentMatchers.any(TransactionEntity.class));
	}

	private TransactionEntity savedWithdrawalTransaction() {
		return new TransactionEntity(
				100L,
				1L,
				null,
				AMOUNT,
				TransactionEntity.Type.WITHDRAWAL,
				TransactionEntity.Status.COMPLETED,
				CREATED_AT
		);
	}
}
