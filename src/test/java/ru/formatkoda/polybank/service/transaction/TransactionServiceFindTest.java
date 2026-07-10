package ru.formatkoda.polybank.service.transaction;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.repository.TransactionRepository;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.service.TransactionService;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.formatkoda.polybank.testutil.TestData.ACCOUNT_NUMBER;
import static ru.formatkoda.polybank.testutil.TestData.USER_LOGIN;
import static ru.formatkoda.polybank.testutil.TestData.account;

@ExtendWith(MockitoExtension.class)
class TransactionServiceFindTest {
	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private AccountService accountService;
	@InjectMocks
	private TransactionService transactionService;

	@ParameterizedTest
	@ValueSource(longs = {0, 5})
	void shouldReturnTransactionsWhenAccountBelongsToUser(Long size) {
		PageRequest pageRequest = new PageRequest(0, 20);

		AccountEntity account = account();
		List<TransactionWithAccountNumbersView> transactions = buildTransactionWithAccountNumbersViews(size);

		PageResult<TransactionWithAccountNumbersView> expectedResult = new PageResult<>(
				transactions,
				pageRequest.page(),
				pageRequest.size(),
				transactions.size()
		);

		when(accountService.findOwnedAccount(ACCOUNT_NUMBER, USER_LOGIN)).thenReturn(account);
		when(transactionRepository.findViewsByAccountId(account.id(), pageRequest)).thenReturn(transactions);
		when(transactionRepository.countAll(account.id())).thenReturn(size);

		PageResult<TransactionWithAccountNumbersView> actualResult = transactionService.findByAccountNumber(
				ACCOUNT_NUMBER,
				USER_LOGIN,
				pageRequest
		);

		assertThat(actualResult).isEqualTo(expectedResult);

		verify(accountService).findOwnedAccount(ACCOUNT_NUMBER, USER_LOGIN);
		verify(transactionRepository).findViewsByAccountId(account.id(), pageRequest);
		verify(transactionRepository).countAll(account.id());
	}

	private List<TransactionWithAccountNumbersView> buildTransactionWithAccountNumbersViews(Long size) {
		List<TransactionWithAccountNumbersView> transactions = new ArrayList<>();

		for (long i = 0; i < size; i++)
			transactions.add(
					new TransactionWithAccountNumbersView(
							i,
							new AccountNumber("67675678901234567890"),
							new AccountNumber("67670000000000000000"),
							new BigDecimal("100.50"),
							TransactionEntity.Type.DEPOSIT,
							TransactionEntity.Status.COMPLETED,
							OffsetDateTime.of(2026, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC)
					)
			);

		return transactions;
	}
}
