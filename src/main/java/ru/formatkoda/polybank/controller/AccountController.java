package ru.formatkoda.polybank.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.dto.transaction.TransactionResponseDto;
import ru.formatkoda.polybank.service.TransactionService;
import ru.formatkoda.polybank.util.mappers.TransactionMapper;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResponse;
import ru.formatkoda.polybank.util.pagination.PageResult;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
	private final TransactionService transactionService;

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/{accountNumber}/transactions")
	public PageResponse<TransactionResponseDto> getAllTransactionsByAccountNumber(
			@PathVariable String accountNumber,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			Authentication authentication
	) {
		UserLogin login = new UserLogin(authentication.getName());
		AccountNumber number = new AccountNumber(accountNumber);
		PageRequest pageRequest = new PageRequest(page, size);

		PageResult<TransactionEntity> result = transactionService.findByAccountNumber(login, number, pageRequest);

		return new PageResponse<>(
				result.items()
						.stream()
						.map(TransactionMapper::toResponse)
						.toList(),
				result.page(),
				result.size(),
				result.total()
		);
	}
}
