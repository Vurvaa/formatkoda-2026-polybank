package ru.formatkoda.polybank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.repository.AccountRepository;
import ru.formatkoda.polybank.repository.TransactionRepository;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	@Mock
	TransactionRepository transactionRepository;
	@Mock
	AccountRepository accountRepository;
	@InjectMocks
	TransactionService transactionService;

	@ParameterizedTest
	@ValueSource(ints = {0, 5})
	void shouldReturnTransactionsWhenAccountBelongsToUser(int size) {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("12345678901234567890");
		PageRequest pageRequest = new PageRequest(0, 20);

		PageResult<TransactionEntity> expectedResult = new PageResult<>(
				buildTransactionEntities(size),
				0,
				20,
				0
		);

		when(accountRepository.existsByNumberAndUserLogin(userLogin, accountNumber)).thenReturn(true);
		when(transactionRepository.findAllByAccountNumber(accountNumber, pageRequest)).thenReturn(expectedResult);

		PageResult<TransactionEntity> actualResult = transactionService.findByAccountNumber(
				userLogin,
				accountNumber,
				pageRequest
		);

		assertSame(expectedResult, actualResult);

		verify(accountRepository).existsByNumberAndUserLogin(userLogin, accountNumber);
		verify(transactionRepository).findAllByAccountNumber(accountNumber, pageRequest);
	}

	@Test
	void shouldThrowExceptionWhenAccountNotBelongsToUser() {
		UserLogin userLogin = new UserLogin("user@example.com");
		AccountNumber accountNumber = new AccountNumber("12345678901234567890");
		PageRequest pageRequest = new PageRequest(0, 20);

		when(accountRepository.existsByNumberAndUserLogin(userLogin, accountNumber))
				.thenReturn(false);

		Throwable exception = assertThrows(AccessDeniedException.class,
				() -> transactionService.findByAccountNumber(userLogin, accountNumber, pageRequest));
		assertEquals("account does not belong to current user", exception.getMessage());

		verify(accountRepository).existsByNumberAndUserLogin(userLogin, accountNumber);
		verifyNoInteractions(transactionRepository);
	}

	private List<TransactionEntity> buildTransactionEntities(int size) {
		List<TransactionEntity> entities = new ArrayList<>();
		for (int i = 0; i < size; i++)
			entities.add(
					new TransactionEntity(
							i,
							new AccountEntity(),
							new AccountEntity(),
							new BigDecimal("100.5"),
							TransactionEntity.Type.DEPOSIT,
							TransactionEntity.Status.COMPLETED,
							OffsetDateTime.MIN
					)
			);

		return entities;
	}
}
