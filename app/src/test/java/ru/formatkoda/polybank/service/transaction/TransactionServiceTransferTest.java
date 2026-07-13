package ru.formatkoda.polybank.service.transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.messaging.publisher.TransactionEventPublisher;
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
import static ru.formatkoda.polybank.testutil.TestData.ACCOUNT_NUMBER;
import static ru.formatkoda.polybank.testutil.TestData.AMOUNT;
import static ru.formatkoda.polybank.testutil.TestData.CREATED_AT;
import static ru.formatkoda.polybank.testutil.TestData.USER_LOGIN;
import static ru.formatkoda.polybank.testutil.TestData.account;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTransferTest {
	private static final AccountNumber TO_ACCOUNT_NUMBER = new AccountNumber("67670000000000000000");

	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private AccountService accountService;
	@Mock
	private TransactionEventPublisher transactionEventPublisher;
	@InjectMocks
	private TransactionService transactionService;

	@Test
	void transferShouldMoveMoneyCreateTransferTransactionAndReturnView() {
		AccountEntity fromAccount = account();
		AccountEntity toAccount = toAccount();

		when(accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.thenReturn(fromAccount);
		when(accountService.topUpAccount(TO_ACCOUNT_NUMBER, AMOUNT))
				.thenReturn(toAccount);
		when(transactionRepository.save(ArgumentMatchers.any(TransactionEntity.class)))
				.thenReturn(savedTransferTransaction());

		TransactionWithAccountNumbersView result = transactionService.transfer(
				ACCOUNT_NUMBER,
				TO_ACCOUNT_NUMBER,
				AMOUNT,
				USER_LOGIN
		);

		assertThat(result.id()).isEqualTo(100L);
		assertThat(result.fromAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
		assertThat(result.toAccountNumber()).isEqualTo(TO_ACCOUNT_NUMBER);
		assertThat(result.amount()).isEqualByComparingTo(AMOUNT);
		assertThat(result.type()).isEqualTo(TransactionEntity.Type.TRANSFER);
		assertThat(result.status()).isEqualTo(TransactionEntity.Status.COMPLETED);
		assertThat(result.createdAt()).isEqualTo(CREATED_AT);

		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);

		verify(transactionRepository).save(transactionCaptor.capture());

		TransactionEntity transactionToSave = transactionCaptor.getValue();

		assertThat(transactionToSave.id()).isNull();
		assertThat(transactionToSave.fromAccountId()).isEqualTo(fromAccount.id());
		assertThat(transactionToSave.toAccountId()).isEqualTo(toAccount.id());
		assertThat(transactionToSave.amount()).isEqualByComparingTo(AMOUNT);
		assertThat(transactionToSave.type()).isEqualTo(TransactionEntity.Type.TRANSFER);
		assertThat(transactionToSave.status()).isEqualTo(TransactionEntity.Status.COMPLETED);
		assertThat(transactionToSave.createdAt()).isNotNull();

		verify(accountService).withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN);
		verify(accountService).topUpAccount(TO_ACCOUNT_NUMBER, AMOUNT);
		verify(transactionEventPublisher).publishTransactionCreated(result);
	}

	@Test
	void transferShouldRejectSameAccountAndNotChangeBalancesOrCreateTransaction() {
		assertThatThrownBy(() -> transactionService.transfer(
				ACCOUNT_NUMBER,
				ACCOUNT_NUMBER,
				AMOUNT,
				USER_LOGIN
		))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("accounts must be different");

		verifyNoInteractions(accountService);
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void transferShouldPropagateWithdrawExceptionAndNotTopUpOrCreateTransaction() {
		when(accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.thenThrow(new BusinessLogicException("insufficient funds"));

		assertThatThrownBy(() -> transactionService.transfer(
				ACCOUNT_NUMBER,
				TO_ACCOUNT_NUMBER,
				AMOUNT,
				USER_LOGIN
		))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("insufficient funds");

		verify(accountService).withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN);
		verify(accountService, never()).topUpOwnedAccount(TO_ACCOUNT_NUMBER, AMOUNT, USER_LOGIN);
		verify(transactionRepository, never()).save(ArgumentMatchers.any());
	}

	@Test
	void transferShouldPropagateTopUpExceptionAndNotCreateTransaction() {
		when(accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.thenReturn(account());
		when(accountService.topUpAccount(TO_ACCOUNT_NUMBER, AMOUNT))
				.thenThrow(new BusinessLogicException("account is inactive"));

		assertThatThrownBy(() -> transactionService.transfer(
				ACCOUNT_NUMBER,
				TO_ACCOUNT_NUMBER,
				AMOUNT,
				USER_LOGIN
		))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("account is inactive");

		verify(accountService).withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN);
		verify(accountService).topUpAccount(TO_ACCOUNT_NUMBER, AMOUNT);
		verify(transactionRepository, never()).save(ArgumentMatchers.any());
	}

	@Test
	void transferShouldPropagateExceptionWhenTransactionCreationFails() {
		when(accountService.withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN))
				.thenReturn(account());
		when(accountService.topUpAccount(TO_ACCOUNT_NUMBER, AMOUNT))
				.thenReturn(toAccount());
		when(transactionRepository.save(ArgumentMatchers.any(TransactionEntity.class)))
				.thenThrow(new RuntimeException("transaction insert failed"));

		assertThatThrownBy(() -> transactionService.transfer(
				ACCOUNT_NUMBER,
				TO_ACCOUNT_NUMBER,
				AMOUNT,
				USER_LOGIN
		))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("transaction insert failed");

		verify(accountService).withdrawFromOwnedAccount(ACCOUNT_NUMBER, AMOUNT, USER_LOGIN);
		verify(accountService).topUpAccount(TO_ACCOUNT_NUMBER, AMOUNT);
		verify(transactionRepository).save(ArgumentMatchers.any(TransactionEntity.class));
	}

	private AccountEntity toAccount() {
		return new AccountEntity(
				2L,
				TO_ACCOUNT_NUMBER,
				10L,
				new BigDecimal("50.00"),
				AccountEntity.Type.CURRENT,
				AccountEntity.Status.ACTIVE,
				CREATED_AT
		);
	}

	private TransactionEntity savedTransferTransaction() {
		return new TransactionEntity(
				100L,
				1L,
				2L,
				AMOUNT,
				TransactionEntity.Type.TRANSFER,
				TransactionEntity.Status.COMPLETED,
				CREATED_AT
		);
	}
}
