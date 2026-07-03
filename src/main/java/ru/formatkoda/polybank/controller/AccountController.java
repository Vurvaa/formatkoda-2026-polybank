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

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
	private final AccountService accountService;

	@PreAuthorize("isAuthenticated()")
	@PostMapping
	public ResponseEntity<CreateAccountResponseDto> createAccount(
			@AuthenticationPrincipal UserSession userSession,
			@Valid @RequestBody CreateAccountRequestDto createAccountDto
	) {
		AccountEntity accountEntity = accountService.createAccountForUser(userSession, createAccountDto.accountType());

		return ResponseEntity.status(HttpStatus.CREATED).body(AccountMapper.toCreateAccountResponseDto(accountEntity));
	}
}
