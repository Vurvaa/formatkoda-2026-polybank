package ru.formatkoda.polybank.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.dto.CreateAccountRequestDto;
import ru.formatkoda.polybank.dto.CreateAccountResponseDto;
import ru.formatkoda.polybank.security.UserSession;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.util.mappers.AccountMapper;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.dto.transaction.TopUpRequestDto;
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
	private final AccountService accountService;
	private final TransactionService transactionService;

	@PreAuthorize("isAuthenticated()")
	@PostMapping
	public ResponseEntity<CreateAccountResponseDto> createAccount(
			@AuthenticationPrincipal UserSession userSession,
			@Valid @RequestBody CreateAccountRequestDto createAccountDto
	) {
		AccountEntity accountEntity = accountService.createAccountForUser(userSession, createAccountDto.accountType());

		return ResponseEntity.status(HttpStatus.CREATED).body(AccountMapper.toCreateAccountResponseDto(accountEntity));
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/{accountNumber}/transactions")
	public ResponseEntity<PageResponse<TransactionResponseDto>> getAllTransactionsByAccountNumber(
			@PathVariable String accountNumber,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			Authentication authentication
	) {
		UserLogin login = new UserLogin(authentication.getName());
		AccountNumber number = new AccountNumber(accountNumber);
		PageRequest pageRequest = new PageRequest(page, size);

		PageResult<TransactionWithAccountNumbersView> result = transactionService
				.findByAccountNumber(number, login, pageRequest);

		return ResponseEntity.ok(
				new PageResponse<>(
						result.items()
								.stream()
								.map(TransactionMapper::toResponse)
								.toList(),
						result.page(),
						result.size(),
						result.total()
				)
		);
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/top-up")
	public ResponseEntity<TransactionResponseDto> topUpAccount(
			@Valid @RequestBody TopUpRequestDto topUpRequestDto,
			Authentication authentication // todo authenticationprincipal when security is done
	) {
		AccountNumber accountNumber = new AccountNumber(topUpRequestDto.accountNumber());
		UserLogin login = new UserLogin(authentication.getName());

		TransactionWithAccountNumbersView transaction = transactionService.topUp(
				accountNumber,
				topUpRequestDto.amount(),
				login
		);

		return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
	}
}
