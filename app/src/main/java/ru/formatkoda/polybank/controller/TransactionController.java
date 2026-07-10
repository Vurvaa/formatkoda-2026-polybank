package ru.formatkoda.polybank.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

import java.math.BigDecimal;

@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionController {

	private final TransactionService transactionService;

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/{accountNumber}")
	public ResponseEntity<PageResponse<TransactionResponseDto>> getAllTransactionsByAccountNumber(
			@AuthenticationPrincipal UserLogin login,
			@PathVariable String accountNumber,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
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
			@AuthenticationPrincipal UserLogin login,
			@Valid @RequestBody AccountOperationRequestDto request
	) {
		AccountNumber accountNumber = new AccountNumber(request.accountNumber());

		TransactionWithAccountNumbersView transaction = transactionService.topUp(
				accountNumber,
				new BigDecimal(request.amount()),
				login
		);

		return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/withdraw")
	public ResponseEntity<TransactionResponseDto> withdrawAccount(
			@AuthenticationPrincipal UserLogin login,
			@Valid @RequestBody AccountOperationRequestDto request
	) {
		AccountNumber accountNumber = new AccountNumber(request.accountNumber());

		TransactionWithAccountNumbersView transaction = transactionService.withdraw(
				accountNumber,
				new BigDecimal(request.amount()),
				login
		);

		return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/transfer")
	public ResponseEntity<TransactionResponseDto> transferBetweenAccounts(
			@AuthenticationPrincipal UserLogin login,
			@Valid @RequestBody TransferRequestDto request
	) {
		AccountNumber fromAccountNumber = new AccountNumber(request.fromAccountNumber());
		AccountNumber toAccountNumber = new AccountNumber(request.toAccountNumber());

		TransactionWithAccountNumbersView transaction = transactionService.transfer(
				fromAccountNumber,
				toAccountNumber,
				new BigDecimal(request.amount()),
				login
		);

		return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
	}

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('MANAGER', 'SENIOR_MANAGER')")
    @PutMapping("/{transactionId}/cancel")
    public ResponseEntity<TransactionResponseDto> cancelTransaction(
            @AuthenticationPrincipal UserLogin managerLogin,
            @PathVariable Long transactionId
    ) {
        TransactionWithAccountNumbersView transaction =
                transactionService.cancelTransaction(
                        managerLogin,
                        transactionId
                );

        return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
    }
}
