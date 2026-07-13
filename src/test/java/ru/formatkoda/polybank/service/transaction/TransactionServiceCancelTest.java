package ru.formatkoda.polybank.service.transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.messaging.publisher.TransactionEventPublisher;
import ru.formatkoda.polybank.repository.TransactionRepository;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.service.TransactionService;
import ru.formatkoda.polybank.service.UserService;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.formatkoda.polybank.testutil.TestData.ACCOUNT_NUMBER;
import static ru.formatkoda.polybank.testutil.TestData.AMOUNT;
import static ru.formatkoda.polybank.testutil.TestData.CREATED_AT;
import static ru.formatkoda.polybank.testutil.TestData.SENIOR_MANAGER_LOGIN;
import static ru.formatkoda.polybank.testutil.TestData.account;
import static ru.formatkoda.polybank.testutil.TestData.userManager;

@ExtendWith(MockitoExtension.class)
class TransactionServiceCancelTest {
	private static final long TRANSACTION_ID = 100L;
	private static final AccountNumber TO_ACCOUNT_NUMBER = new AccountNumber("67670000000000000000");

	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private AccountService accountService;
	@Mock
	private UserService userService;
	@Mock
	private TransactionEventPublisher transactionEventPublisher;
	@InjectMocks
	private TransactionService transactionService;

	@Test
	void cancelTransactionShouldCancelTransferMoveMoneyBackCreateRefundAndReturnView() {
		TransactionEntity transaction = completedTransferTransaction();
		AccountEntity fromAccount = account();
		AccountEntity toAccount = toAccount();
		TransactionEntity canceledTransaction = transactionWithStatus(transaction, TransactionEntity.Status.CANCELED);
		TransactionEntity savedRefund = savedRefundTransaction(toAccount.id(), fromAccount.id());

		when(userService.findUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
		when(transactionRepository.changeTransactionStatus(
				TRANSACTION_ID,
				TransactionEntity.Status.COMPLETED,
				TransactionEntity.Status.CANCELED
		)).thenReturn(Optional.of(canceledTransaction));
		when(accountService.findAccountOrThrow(fromAccount.id())).thenReturn(fromAccount);
		when(accountService.topUpAccount(ACCOUNT_NUMBER, AMOUNT)).thenReturn(fromAccount);
		when(accountService.findAccountOrThrow(toAccount.id())).thenReturn(toAccount);
		when(accountService.withdrawFromAccount(TO_ACCOUNT_NUMBER, AMOUNT)).thenReturn(toAccount);
		when(transactionRepository.save(ArgumentMatchers.any(TransactionEntity.class))).thenReturn(savedRefund);

		TransactionWithAccountNumbersView result = transactionService.cancelTransaction(
				SENIOR_MANAGER_LOGIN,
				TRANSACTION_ID
		);

		assertThat(result.id()).isEqualTo(savedRefund.id());
		assertThat(result.fromAccountNumber()).isEqualTo(TO_ACCOUNT_NUMBER);
		assertThat(result.toAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
		assertThat(result.amount()).isEqualByComparingTo(AMOUNT);
		assertThat(result.type()).isEqualTo(TransactionEntity.Type.REFUND);
		assertThat(result.status()).isEqualTo(TransactionEntity.Status.COMPLETED);
		assertThat(result.createdAt()).isEqualTo(CREATED_AT);

		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
		verify(transactionRepository).save(transactionCaptor.capture());

		TransactionEntity transactionToSave = transactionCaptor.getValue();
		assertThat(transactionToSave.id()).isNull();
		assertThat(transactionToSave.fromAccountId()).isEqualTo(toAccount.id());
		assertThat(transactionToSave.toAccountId()).isEqualTo(fromAccount.id());
		assertThat(transactionToSave.amount()).isEqualByComparingTo(AMOUNT);
		assertThat(transactionToSave.type()).isEqualTo(TransactionEntity.Type.REFUND);
		assertThat(transactionToSave.status()).isEqualTo(TransactionEntity.Status.COMPLETED);
		assertThat(transactionToSave.createdAt()).isNotNull();

		verify(accountService).topUpAccount(ACCOUNT_NUMBER, AMOUNT);
		verify(accountService).withdrawFromAccount(TO_ACCOUNT_NUMBER, AMOUNT);
		verify(transactionEventPublisher).publishTransactionCreated(result);
	}

	@Test
	void cancelTransactionShouldChangeOriginalStatusBeforeMovingMoney() {
		TransactionEntity transaction = completedTransferTransaction();
		AccountEntity fromAccount = account();
		AccountEntity toAccount = toAccount();

		when(userService.findUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
		when(transactionRepository.changeTransactionStatus(
				TRANSACTION_ID,
				TransactionEntity.Status.COMPLETED,
				TransactionEntity.Status.CANCELED
		)).thenReturn(Optional.of(transactionWithStatus(transaction, TransactionEntity.Status.CANCELED)));
		when(accountService.findAccountOrThrow(fromAccount.id())).thenReturn(fromAccount);
		when(accountService.topUpAccount(ACCOUNT_NUMBER, AMOUNT)).thenReturn(fromAccount);
		when(accountService.findAccountOrThrow(toAccount.id())).thenReturn(toAccount);
		when(accountService.withdrawFromAccount(TO_ACCOUNT_NUMBER, AMOUNT)).thenReturn(toAccount);
		when(transactionRepository.save(ArgumentMatchers.any(TransactionEntity.class)))
				.thenReturn(savedRefundTransaction(toAccount.id(), fromAccount.id()));

		var result = transactionService.cancelTransaction(SENIOR_MANAGER_LOGIN, TRANSACTION_ID);

		InOrder inOrder = inOrder(transactionRepository, accountService);
		inOrder.verify(transactionRepository).changeTransactionStatus(
				TRANSACTION_ID,
				TransactionEntity.Status.COMPLETED,
				TransactionEntity.Status.CANCELED
		);
		inOrder.verify(accountService).findAccountOrThrow(fromAccount.id());
		inOrder.verify(accountService).topUpAccount(ACCOUNT_NUMBER, AMOUNT);
		inOrder.verify(accountService).findAccountOrThrow(toAccount.id());
		inOrder.verify(accountService).withdrawFromAccount(TO_ACCOUNT_NUMBER, AMOUNT);
		inOrder.verify(transactionRepository).save(ArgumentMatchers.any(TransactionEntity.class));
		verify(transactionEventPublisher).publishTransactionCreated(result);
	}

	@Test
	void cancelTransactionShouldBeTransactional() throws NoSuchMethodException {
		Method method = TransactionService.class.getMethod(
				"cancelTransaction",
				ru.formatkoda.polybank.domain.user.UserLogin.class,
				Long.class
		);

		Transactional transactional = method.getAnnotation(Transactional.class);

		assertThat(transactional).isNotNull();
		assertThat(transactional.readOnly()).isFalse();
	}

	@Test
	void cancelWithdrawalShouldTopUpOriginalSenderAndCreateRefundFromAtm() {
		TransactionEntity transaction = completedWithdrawalTransaction();
		AccountEntity fromAccount = account();
		TransactionEntity savedRefund = savedRefundTransaction(null, fromAccount.id());

		when(userService.findUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
		when(transactionRepository.changeTransactionStatus(
				TRANSACTION_ID,
				TransactionEntity.Status.COMPLETED,
				TransactionEntity.Status.CANCELED
		)).thenReturn(Optional.of(transactionWithStatus(transaction, TransactionEntity.Status.CANCELED)));
		when(accountService.findAccountOrThrow(fromAccount.id())).thenReturn(fromAccount);
		when(accountService.topUpAccount(ACCOUNT_NUMBER, AMOUNT)).thenReturn(fromAccount);
		when(transactionRepository.save(ArgumentMatchers.any(TransactionEntity.class))).thenReturn(savedRefund);

		TransactionWithAccountNumbersView result = transactionService.cancelTransaction(
				SENIOR_MANAGER_LOGIN,
				TRANSACTION_ID
		);

		assertThat(result.fromAccountNumber()).isNull();
		assertThat(result.toAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
		assertThat(result.type()).isEqualTo(TransactionEntity.Type.REFUND);

		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
		verify(transactionRepository).save(transactionCaptor.capture());

		assertThat(transactionCaptor.getValue().fromAccountId()).isNull();
		assertThat(transactionCaptor.getValue().toAccountId()).isEqualTo(fromAccount.id());
		verify(accountService).topUpAccount(ACCOUNT_NUMBER, AMOUNT);
		verify(accountService, never()).withdrawFromAccount(ArgumentMatchers.any(), ArgumentMatchers.any());
		verify(transactionEventPublisher).publishTransactionCreated(result);
	}

	@Test
	void cancelDepositShouldWithdrawFromOriginalReceiverAndCreateRefundToAtm() {
		TransactionEntity transaction = completedDepositTransaction();
		AccountEntity toAccount = account();
		TransactionEntity savedRefund = savedRefundTransaction(toAccount.id(), null);

		when(userService.findUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
		when(transactionRepository.changeTransactionStatus(
				TRANSACTION_ID,
				TransactionEntity.Status.COMPLETED,
				TransactionEntity.Status.CANCELED
		)).thenReturn(Optional.of(transactionWithStatus(transaction, TransactionEntity.Status.CANCELED)));
		when(accountService.findAccountOrThrow(toAccount.id())).thenReturn(toAccount);
		when(accountService.withdrawFromAccount(ACCOUNT_NUMBER, AMOUNT)).thenReturn(toAccount);
		when(transactionRepository.save(ArgumentMatchers.any(TransactionEntity.class))).thenReturn(savedRefund);

		TransactionWithAccountNumbersView result = transactionService.cancelTransaction(
				SENIOR_MANAGER_LOGIN,
				TRANSACTION_ID
		);

		assertThat(result.fromAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
		assertThat(result.toAccountNumber()).isNull();
		assertThat(result.type()).isEqualTo(TransactionEntity.Type.REFUND);

		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
		verify(transactionRepository).save(transactionCaptor.capture());

		assertThat(transactionCaptor.getValue().fromAccountId()).isEqualTo(toAccount.id());
		assertThat(transactionCaptor.getValue().toAccountId()).isNull();
		verify(accountService, never()).topUpAccount(ArgumentMatchers.any(), ArgumentMatchers.any());
		verify(accountService).withdrawFromAccount(ACCOUNT_NUMBER, AMOUNT);
		verify(transactionEventPublisher).publishTransactionCreated(result);
	}

	@Test
	void cancelTransactionShouldThrowWhenTransactionNotFound() {
		when(userService.findUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.cancelTransaction(SENIOR_MANAGER_LOGIN, TRANSACTION_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("transaction not found");

		verify(transactionRepository).findById(TRANSACTION_ID);
		verify(transactionRepository, never()).changeTransactionStatus(
				ArgumentMatchers.anyLong(),
				ArgumentMatchers.any(),
				ArgumentMatchers.any()
		);
		verifyNoInteractions(accountService);
	}

	@Test
	void cancelTransactionShouldThrowWhenTransactionIsNotCompleted() {
		TransactionEntity transaction = transactionWithStatus(
				completedTransferTransaction(),
				TransactionEntity.Status.FAILED
		);

		when(userService.findUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));

		assertThatThrownBy(() -> transactionService.cancelTransaction(SENIOR_MANAGER_LOGIN, TRANSACTION_ID))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("transaction must be in completed state to cancel");

		verify(transactionRepository, never()).changeTransactionStatus(
				ArgumentMatchers.anyLong(),
				ArgumentMatchers.any(),
				ArgumentMatchers.any()
		);
		verifyNoInteractions(accountService);
	}

	@Test
	void cancelTransactionShouldThrowWhenTransactionIsAlreadyCanceled() {
		TransactionEntity transaction = transactionWithStatus(
				completedTransferTransaction(),
				TransactionEntity.Status.CANCELED
		);

		when(userService.findUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));

		assertThatThrownBy(() -> transactionService.cancelTransaction(SENIOR_MANAGER_LOGIN, TRANSACTION_ID))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("transaction must be in completed state to cancel");

		verify(transactionRepository, never()).changeTransactionStatus(
				ArgumentMatchers.anyLong(),
				ArgumentMatchers.any(),
				ArgumentMatchers.any()
		);
		verifyNoInteractions(accountService);
	}

	@Test
	void cancelTransactionShouldThrowWhenStatusWasChangedConcurrently() {
		TransactionEntity transaction = completedTransferTransaction();

		when(userService.findUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
		when(transactionRepository.changeTransactionStatus(
				TRANSACTION_ID,
				TransactionEntity.Status.COMPLETED,
				TransactionEntity.Status.CANCELED
		)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.cancelTransaction(SENIOR_MANAGER_LOGIN, TRANSACTION_ID))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("transaction refund not completed");

		verify(transactionRepository).changeTransactionStatus(
				TRANSACTION_ID,
				TransactionEntity.Status.COMPLETED,
				TransactionEntity.Status.CANCELED
		);
		verifyNoInteractions(accountService);
		verify(transactionRepository, never()).save(ArgumentMatchers.any(TransactionEntity.class));
	}

	@Test
	void cancelTransactionShouldThrowWhenTransactionIsRefund() {
		TransactionEntity transaction = new TransactionEntity(
				TRANSACTION_ID,
				2L,
				1L,
				AMOUNT,
				TransactionEntity.Type.REFUND,
				TransactionEntity.Status.COMPLETED,
				CREATED_AT
		);

		when(userService.findUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));

		assertThatThrownBy(() -> transactionService.cancelTransaction(SENIOR_MANAGER_LOGIN, TRANSACTION_ID))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("refund transaction cannot be cancelled");

		verify(transactionRepository, never()).changeTransactionStatus(
				ArgumentMatchers.anyLong(),
				ArgumentMatchers.any(),
				ArgumentMatchers.any()
		);
		verifyNoInteractions(accountService);
	}

	@Test
	void cancelTransactionShouldThrowWhenManagerIsBlocked() {
		when(userService.findUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(blockedManager());

		assertThatThrownBy(() -> transactionService.cancelTransaction(SENIOR_MANAGER_LOGIN, TRANSACTION_ID))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("manager is blocked");

		verifyNoInteractions(transactionRepository, accountService);
	}

	@Test
	void cancelTransactionShouldPropagateBalanceOperationExceptionAndNotCreateRefundTransaction() {
		TransactionEntity transaction = completedDepositTransaction();
		AccountEntity toAccount = account();

		when(userService.findUserByLogin(SENIOR_MANAGER_LOGIN)).thenReturn(userManager());
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
		when(transactionRepository.changeTransactionStatus(
				TRANSACTION_ID,
				TransactionEntity.Status.COMPLETED,
				TransactionEntity.Status.CANCELED
		)).thenReturn(Optional.of(transactionWithStatus(transaction, TransactionEntity.Status.CANCELED)));
		when(accountService.findAccountOrThrow(toAccount.id())).thenReturn(toAccount);
		when(accountService.withdrawFromAccount(ACCOUNT_NUMBER, AMOUNT))
				.thenThrow(new BusinessLogicException("insufficient funds"));

		assertThatThrownBy(() -> transactionService.cancelTransaction(SENIOR_MANAGER_LOGIN, TRANSACTION_ID))
				.isInstanceOf(BusinessLogicException.class)
				.hasMessage("insufficient funds");

		verify(transactionRepository).changeTransactionStatus(
				TRANSACTION_ID,
				TransactionEntity.Status.COMPLETED,
				TransactionEntity.Status.CANCELED
		);
		verify(transactionRepository, never()).save(ArgumentMatchers.any(TransactionEntity.class));
	}

	private TransactionEntity completedTransferTransaction() {
		return new TransactionEntity(
				TRANSACTION_ID,
				1L,
				2L,
				AMOUNT,
				TransactionEntity.Type.TRANSFER,
				TransactionEntity.Status.COMPLETED,
				CREATED_AT
		);
	}

	private TransactionEntity completedWithdrawalTransaction() {
		return new TransactionEntity(
				TRANSACTION_ID,
				1L,
				null,
				AMOUNT,
				TransactionEntity.Type.WITHDRAWAL,
				TransactionEntity.Status.COMPLETED,
				CREATED_AT
		);
	}

	private TransactionEntity completedDepositTransaction() {
		return new TransactionEntity(
				TRANSACTION_ID,
				null,
				1L,
				AMOUNT,
				TransactionEntity.Type.DEPOSIT,
				TransactionEntity.Status.COMPLETED,
				CREATED_AT
		);
	}

	private TransactionEntity transactionWithStatus(
			TransactionEntity transaction,
			TransactionEntity.Status status
	) {
		return new TransactionEntity(
				transaction.id(),
				transaction.fromAccountId(),
				transaction.toAccountId(),
				transaction.amount(),
				transaction.type(),
				status,
				transaction.createdAt()
		);
	}

	private TransactionEntity savedRefundTransaction(Long fromAccountId, Long toAccountId) {
		return new TransactionEntity(
				200L,
				fromAccountId,
				toAccountId,
				AMOUNT,
				TransactionEntity.Type.REFUND,
				TransactionEntity.Status.COMPLETED,
				CREATED_AT
		);
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

	private UserEntity blockedManager() {
		return new UserEntity(
				25L,
				SENIOR_MANAGER_LOGIN,
				"User",
				"Test",
				"password-hash",
				CREATED_AT,
				CREATED_AT
		);
	}
}
