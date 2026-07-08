package ru.formatkoda.polybank.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.dto.account.AccountResponseDto;
import ru.formatkoda.polybank.dto.account.CreateAccountRequestDto;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.util.mapper.AccountMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.user.UserLogin;

import java.util.List;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
	private final AccountService accountService;

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping
	public ResponseEntity<AccountResponseDto> createAccount(
			@AuthenticationPrincipal UserLogin login,
			@Valid @RequestBody CreateAccountRequestDto createAccountDto
	) {
		AccountEntity accountEntity = accountService.createAccountForUser(createAccountDto.accountType(), login);

		return ResponseEntity.status(HttpStatus.CREATED).body(AccountMapper.toResponse(accountEntity));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@GetMapping
	public ResponseEntity<List<AccountResponseDto>> getAllAccounts(@AuthenticationPrincipal UserLogin login) {
		List<AccountResponseDto> accounts = accountService
				.findAllForUser(login)
				.stream()
				.map(AccountMapper::toResponse)
				.toList();

		return ResponseEntity.ok(accounts);
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/{accountNumber}")
	public ResponseEntity<AccountResponseDto> getAccount(
			@AuthenticationPrincipal UserLogin login,
			@PathVariable String accountNumber
	) {
		AccountEntity account = accountService.findOwnedAccount(new AccountNumber(accountNumber), login);

		return ResponseEntity.ok(AccountMapper.toResponse(account));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PutMapping("/{number}/close")
	public ResponseEntity<AccountResponseDto> closeAccount(
			@AuthenticationPrincipal UserLogin userLogin,
			@PathVariable(name = "number") String accountNumber
	) {
		AccountEntity accountEntity = accountService
				.closeAccountForUser(
						userLogin,
						new AccountNumber(String.valueOf(accountNumber)
						)
				);

		return ResponseEntity.ok(AccountMapper.toResponse(accountEntity));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PutMapping("/{number}/freeze")
	public ResponseEntity<AccountResponseDto> freezeAccount(
			@AuthenticationPrincipal UserLogin userLogin,
			@PathVariable(name = "number") String accountNumber
	) {
		AccountEntity accountEntity = accountService
				.freezeAccountForUser(
						userLogin,
						new AccountNumber(String.valueOf(accountNumber)
						)
				);

		return ResponseEntity.ok(AccountMapper.toResponse(accountEntity));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PutMapping("/{number}/unfreeze")
	public ResponseEntity<AccountResponseDto> unFreezeAccount(
			@AuthenticationPrincipal UserLogin userLogin,
			@PathVariable(name = "number") String accountNumber
	) {
		AccountEntity accountEntity = accountService
				.unFreezeAccountForUser(
						userLogin,
						new AccountNumber(String.valueOf(accountNumber)
						)
				);

		return ResponseEntity.ok(AccountMapper.toResponse(accountEntity));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("hasAnyRole('MANAGER', 'SENIOR_MANAGER')")
	@PutMapping("/{number}/block")
	public ResponseEntity<AccountResponseDto> blockAccount(
			@Valid @AuthenticationPrincipal UserLogin managerLogin,
			@PathVariable(name = "number") String accountNumber
	) {
		AccountEntity accountEntity = accountService.blockAccountByNumber(
				managerLogin,
				new AccountNumber(accountNumber)
		);

		return ResponseEntity.ok(AccountMapper.toResponse(accountEntity));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("hasAnyRole('MANAGER', 'SENIOR_MANAGER')")
	@PutMapping("/{number}/unblock")
	public ResponseEntity<AccountResponseDto> unBlockAccount(
			@Valid @AuthenticationPrincipal UserLogin managerLogin,
			@PathVariable(name = "number") String accountNumber
	) {
		AccountEntity accountEntity = accountService.unBlockAccountByNumber(
				managerLogin,
				new AccountNumber(accountNumber)
		);

		return ResponseEntity.ok(AccountMapper.toResponse(accountEntity));
	}
}
