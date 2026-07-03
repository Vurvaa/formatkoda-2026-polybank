package ru.formatkoda.polybank.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.dto.account.CreateAccountRequestDto;
import ru.formatkoda.polybank.dto.account.CreateAccountResponseDto;
import ru.formatkoda.polybank.security.UserSession;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.util.mapper.AccountMapper;
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
import ru.formatkoda.polybank.dto.transaction.AccountOperationRequestDto;
import ru.formatkoda.polybank.dto.transaction.TransactionResponseDto;
import ru.formatkoda.polybank.dto.transaction.TransferRequestDto;
import ru.formatkoda.polybank.service.TransactionService;
import ru.formatkoda.polybank.util.mapper.TransactionMapper;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResponse;
import ru.formatkoda.polybank.util.pagination.PageResult;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
	private final AccountService accountService;
	private final TransactionService transactionService;

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping
	public ResponseEntity<CreateAccountResponseDto> createAccount(
			@AuthenticationPrincipal UserSession userSession,
			@Valid @RequestBody CreateAccountRequestDto createAccountDto
	) {
		UserLogin login = new UserLogin(userSession.login());
		AccountEntity accountEntity = accountService.createAccountForUser(createAccountDto.accountType(), login);

		return ResponseEntity.status(HttpStatus.CREATED).body(AccountMapper.toCreateAccountResponseDto(accountEntity));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/{accountNumber}/transactions")
	public ResponseEntity<PageResponse<TransactionResponseDto>> getAllTransactionsByAccountNumber(
			@AuthenticationPrincipal UserSession userSession,
			@PathVariable String accountNumber,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		UserLogin login = new UserLogin(userSession.login());
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

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/top-up")
	public ResponseEntity<TransactionResponseDto> topUpAccount(
			@AuthenticationPrincipal UserSession userSession,
			@Valid @RequestBody AccountOperationRequestDto request
	) {
		UserLogin login = new UserLogin(userSession.login());
		AccountNumber accountNumber = new AccountNumber(request.accountNumber());

		TransactionWithAccountNumbersView transaction = transactionService.topUp(
				accountNumber,
				request.amount(),
				login
		);

		return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/withdraw")
	public ResponseEntity<TransactionResponseDto> withdrawAccount(
			@AuthenticationPrincipal UserSession userSession,
			@Valid @RequestBody AccountOperationRequestDto request
	) {
		UserLogin login = new UserLogin(userSession.login());
		AccountNumber accountNumber = new AccountNumber(request.accountNumber());

		TransactionWithAccountNumbersView transaction = transactionService.withdraw(
				accountNumber,
				request.amount(),
				login
		);

		return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/transfer")
	public ResponseEntity<TransactionResponseDto> transferBetweenAccounts(
			@AuthenticationPrincipal UserSession userSession,
			@Valid @RequestBody TransferRequestDto request
	) {
		UserLogin login = new UserLogin(userSession.login());
		AccountNumber fromAccountNumber = new AccountNumber(request.fromAccountNumber());
		AccountNumber toAccountNumber = new AccountNumber(request.toAccountNumber());

		TransactionWithAccountNumbersView transaction = transactionService.transfer(
				fromAccountNumber,
				toAccountNumber,
				request.amount(),
				login
		);

		return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
	}
}
