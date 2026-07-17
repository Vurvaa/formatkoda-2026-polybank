package ru.formatkoda.polybank.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.messaging.publisher.NotificationEventPublisher;
import ru.formatkoda.polybank.messaging.publisher.TransactionEventPublisher;
import ru.formatkoda.polybank.repository.TransactionRepository;
import ru.formatkoda.polybank.util.mapper.TransactionMapper;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;
import ru.formatkoda.polybank.messaging.publisher.NotificationEventPublisher.NotificationTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static ru.formatkoda.polybank.messaging.outbox.publisher.NotificationOutboxPublisher.AvailableNotificationMethods.EMAIL;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionService {
	private static final String SENIOR_MANAGER = "SENIOR_MANAGER";

	private final TransactionRepository transactionRepository;
	private final AccountService accountService;
	private final UserService userService;
	private final TransactionEventPublisher transactionEventPublisher;
	private final NotificationEventPublisher notificationEventPublisher;

	public PageResult<TransactionWithAccountNumbersView> findByAccountNumber(
			@NonNull AccountNumber accountNumber,
			@NonNull UserLogin userLogin,
			@NonNull PageRequest pageRequest
	) {
		AccountEntity account;
		if (!userService.hasRole(userLogin, SENIOR_MANAGER))
			account = accountService.findOwnedAccount(accountNumber, userLogin);
		else
			 account = accountService.findAccountByAccountNumber(accountNumber);

		List<TransactionWithAccountNumbersView> transactions = transactionRepository
				.findViewsByAccountId(account.id(), pageRequest);
		Long total = transactionRepository.countAll(account.id());

		return new PageResult<>(
				transactions,
				pageRequest.page(),
				pageRequest.size(),
				total
		);
	}

	@Transactional
	public TransactionWithAccountNumbersView topUp(
			@NonNull AccountNumber accountNumber,
			@NonNull BigDecimal amount,
			@NonNull UserLogin userLogin
	) {
		UserEntity user = userService.findUserByLogin(userLogin);

		AccountEntity account = accountService.topUpOwnedAccount(accountNumber, amount, userLogin);

		TransactionEntity transaction = saveCompletedTransaction(
				null,
				account.id(),
				amount,
				TransactionEntity.Type.DEPOSIT
		);

		TransactionWithAccountNumbersView transactionView = TransactionMapper.toView(
				transaction,
				null,
				accountNumber
		);

		transactionEventPublisher.publishTransactionCreated(transactionView);
		notificationEventPublisher.publishTransactionNotificationEvent(
				TransactionMapper.toNotificationDto(
						transactionView,
						null,
						user.id(),
						user.name(),
						user.email().value()
				),
				List.of(EMAIL.name()),
				NotificationTemplate.TRANSACTION_TOP_UP
		);


		return transactionView;
	}

	@Transactional
	public TransactionWithAccountNumbersView withdraw(
			@NonNull AccountNumber accountNumber,
			@NonNull BigDecimal amount,
			@NonNull UserLogin userLogin
	) {
		UserEntity user = userService.findUserByLogin(userLogin);

		AccountEntity account = accountService.withdrawFromOwnedAccount(accountNumber, amount, userLogin);

		TransactionEntity transaction = saveCompletedTransaction(
				account.id(),
				null,
				amount,
				TransactionEntity.Type.WITHDRAWAL
		);

		TransactionWithAccountNumbersView transactionView = TransactionMapper.toView(
				transaction,
				accountNumber,
				null
		);

		transactionEventPublisher.publishTransactionCreated(transactionView);
		notificationEventPublisher.publishTransactionNotificationEvent(
				TransactionMapper.toNotificationDto(
						transactionView,
						user.id(),
						null,
						user.name(),
						user.email().value()
				),
				List.of(EMAIL.name()),
				NotificationTemplate.TRANSACTION_WITHDRAW
		);

		return transactionView;
	}

	@Transactional
	public TransactionWithAccountNumbersView transfer(
			@NonNull AccountNumber fromAccountNumber,
			@NonNull AccountNumber toAccountNumber,
			@NonNull BigDecimal amount,
			@NonNull UserLogin userLogin
	) {
		if (fromAccountNumber.equals(toAccountNumber))
			throw new BusinessLogicException("accounts must be different");

		AccountEntity fromAccount = accountService.withdrawFromOwnedAccount(fromAccountNumber, amount, userLogin);
		AccountEntity toAccount = accountService.topUpAccount(toAccountNumber, amount);

		UserEntity userFrom = userService.findUserById(fromAccount.userId());
		UserEntity userTo = userService.findUserById(toAccount.userId());

		TransactionEntity transaction = saveCompletedTransaction(
				fromAccount.id(),
				toAccount.id(),
				amount,
				TransactionEntity.Type.TRANSFER
		);

		TransactionWithAccountNumbersView transactionView = TransactionMapper.toView(
				transaction,
				fromAccountNumber,
				toAccountNumber
		);

		transactionEventPublisher.publishTransactionCreated(transactionView);
		if (userFrom.id().equals(userTo.id())) {
			String personName = userFrom.name();

			notificationEventPublisher.publishTransactionNotificationEvent(
					TransactionMapper.toNotificationDto(
							transactionView,
							userFrom.id(),
							userTo.id(),
							personName,
							userFrom.email().value()
					),
					List.of(EMAIL.name()),
					NotificationTemplate.TRANSACTION_BETWEEN_PERSON_ACCOUNTS
			);
		} else {
			notificationEventPublisher.publishTransactionNotificationEvent(
					TransactionMapper.toNotificationDto(
							transactionView,
							userFrom.id(),
							userTo.id(),
							userFrom.name(),
							userFrom.email().value()
					),
					List.of(EMAIL.name()),
					NotificationTemplate.TRANSACTION_WITHDRAW_BETWEEN_ACCOUNTS
			);

			notificationEventPublisher.publishTransactionNotificationEvent(
					TransactionMapper.toNotificationDto(
							transactionView,
							userFrom.id(),
							userTo.id(),
							userTo.name(),
							userTo.email().value()
					),
					List.of(EMAIL.name()),
					NotificationTemplate.TRANSACTION_TOP_UP_BETWEEN_ACCOUNTS
			);
		}
		return transactionView;
	}

	public TransactionEntity findTransactionOrThrow(@NonNull Long id) {
		return transactionRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("transaction not found"));
	}

	@Transactional
	public TransactionWithAccountNumbersView cancelTransaction(
			@NonNull UserLogin managerLogin,
			@NonNull Long transactionId
	) {
		UserEntity manager = userService.findUserByLogin(managerLogin);
		if (manager.isBlocked()) {
			throw new BusinessLogicException("manager is blocked");
		}

		TransactionEntity transaction = findTransactionOrThrow(transactionId);
		if (!transaction.status().equals(TransactionEntity.Status.COMPLETED)) {
			throw new BusinessLogicException("transaction must be in completed state to cancel");
		} else if (transaction.type().equals(TransactionEntity.Type.REFUND)) {
			throw new BusinessLogicException("refund transaction cannot be cancelled");
		}

		TransactionEntity updatedTransaction = transactionRepository
				.changeTransactionStatus(
						transactionId,
						TransactionEntity.Status.COMPLETED,
						TransactionEntity.Status.CANCELED
				).orElseThrow(() -> new BusinessLogicException("transaction refund not completed"));

		Long refundAccountToId = null;
		AccountNumber refundToAccountNumber = null;
		if (transaction.fromAccountId() != null) {
			AccountEntity accountTo = accountService.findAccountOrThrow(transaction.fromAccountId());
			accountTo = accountService.topUpAccount(
					accountTo.number(),
					transaction.amount()
			);
			refundAccountToId = accountTo.id();
			refundToAccountNumber = accountTo.number();
		}

		Long refundAccountFromId = null;
		AccountNumber refundFromAccountNumber = null;
		if (transaction.toAccountId() != null) {
			AccountEntity accountFrom = accountService.findAccountOrThrow(transaction.toAccountId());
			accountFrom = accountService.withdrawFromAccount(
					accountFrom.number(),
					transaction.amount()
			);
			refundAccountFromId = accountFrom.id();
			refundFromAccountNumber = accountFrom.number();
		}

		TransactionEntity refundTransaction = saveCompletedTransaction(
				refundAccountFromId,
				refundAccountToId,
				updatedTransaction.amount(),
				TransactionEntity.Type.REFUND
		);

		TransactionWithAccountNumbersView transactionView = TransactionMapper.toView(
				refundTransaction,
				refundFromAccountNumber,
				refundToAccountNumber
		);

		transactionEventPublisher.publishTransactionCreated(transactionView);

		return transactionView;

	}

	private TransactionEntity saveCompletedTransaction(
			Long fromAccountId,
			Long toAccountId,
			BigDecimal amount,
			TransactionEntity.Type type
	) {
		return transactionRepository.save(
				new TransactionEntity(
						null,
						fromAccountId,
						toAccountId,
						amount,
						type,
						TransactionEntity.Status.COMPLETED,
						OffsetDateTime.now(ZoneOffset.UTC)
				)
		);
	}
}
