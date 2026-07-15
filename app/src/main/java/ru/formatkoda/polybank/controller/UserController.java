package ru.formatkoda.polybank.controller;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.formatkoda.polybank.domain.account.AccountInfo;
import ru.formatkoda.polybank.domain.auth.JwtToken;
import ru.formatkoda.polybank.domain.user.UserEmail;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.dto.user.StaffUserRegistrationDto;
import ru.formatkoda.polybank.dto.user.UserDetailsResponseDto;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.dto.user.UserEmailDto;
import ru.formatkoda.polybank.dto.user.UserLoginDto;
import ru.formatkoda.polybank.dto.user.UserRegistrationDto;
import ru.formatkoda.polybank.dto.user.AuthUserDto;
import ru.formatkoda.polybank.dto.user.UserInfoResponseDto;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.service.AuthService;
import ru.formatkoda.polybank.service.UserService;
import ru.formatkoda.polybank.util.mapper.AccountMapper;
import ru.formatkoda.polybank.util.mapper.UserMapper;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResponse;
import ru.formatkoda.polybank.util.pagination.PageResult;

import java.util.List;

@RestController
@RequestMapping(path = "/user")
@RequiredArgsConstructor
public class UserController {
	private final AuthService authService;
	private final UserService userService;
	private final AccountService accountService;
	private final UserMapper userMapper;

	@PostMapping("/sign-up")
	public ResponseEntity<AuthUserDto> registrationUser(@RequestBody @Valid UserRegistrationDto user) {
		JwtToken token = authService.registerUser(userMapper.toEntity(user));

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new AuthUserDto(token.token()));
	}

	@PostMapping("/sign-in")
	public ResponseEntity<AuthUserDto> loginUser(@RequestBody @Valid UserLoginDto user) {
		JwtToken token = authService.loginUser(userMapper.toEntity(user));

		return ResponseEntity.ok(new AuthUserDto(token.token()));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("hasRole('SENIOR_MANAGER')")
	@PutMapping("/{userLogin}/block")
	public ResponseEntity<UserDetailsResponseDto> blockUser(
			@AuthenticationPrincipal UserLogin managerLogin,
			@Valid @PathVariable String userLogin
	) {
		UserWithRolesView user = userService.blockUserByLogin(
				managerLogin,
				new UserLogin(userLogin)
		);

		return ResponseEntity.ok(userMapper.toResponse(user));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("hasRole('SENIOR_MANAGER')")
	@PutMapping("/{userLogin}/unblock")
	public ResponseEntity<UserDetailsResponseDto> unBlockUser(
			@AuthenticationPrincipal UserLogin managerLogin,
			@Valid @PathVariable String userLogin
	) {
		UserWithRolesView user = userService.unBlockUserByLogin(
				managerLogin,
				new UserLogin(userLogin)
		);

		return ResponseEntity.ok(userMapper.toResponse(user));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("hasAnyRole('SENIOR_MANAGER', 'MANAGER')")
	@GetMapping
	public ResponseEntity<PageResponse<UserDetailsResponseDto>> getAllUsers(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		PageRequest pageRequest = new PageRequest(page, size);
		PageResult<UserWithRolesView> result = userService.findAllUsersWithRoles(pageRequest);

		return ResponseEntity.ok(
				new PageResponse<>(
						result.items()
								.stream()
								.map(userMapper::toResponse)
								.toList(),
						result.page(),
						result.size(),
						result.total()

				)
		);
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("hasRole('SENIOR_MANAGER')")
	@DeleteMapping("/{userLogin}/roles/{roleName}")
	public ResponseEntity<UserDetailsResponseDto> removeUserRole(
			@AuthenticationPrincipal UserLogin managerLogin,
			@PathVariable String userLogin,
			@PathVariable String roleName
	) {
		UserWithRolesView user = userService.removeUserRole(
				managerLogin,
				new UserLogin(userLogin),
				roleName
		);

		return ResponseEntity.ok(userMapper.toResponse(user));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("hasRole('SENIOR_MANAGER')")
	@PutMapping("/{userLogin}/roles/{roleName}")
	public ResponseEntity<UserDetailsResponseDto> addUserRole(
			@AuthenticationPrincipal UserLogin managerLogin,
			@PathVariable String userLogin,
			@PathVariable String roleName
	) {
		UserWithRolesView user = userService.addUserRole(
				managerLogin,
				new UserLogin(userLogin),
				roleName
		);

		return ResponseEntity.ok(userMapper.toResponse(user));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/mail")
	public ResponseEntity<UserEmailDto> addUserEmail(
			@AuthenticationPrincipal UserLogin userLogin,
			@RequestBody @Valid UserEmailDto userEmail
	) {
		UserEmail email = new UserEmail(userEmail.value());
		UserEntity user = userService.addUserEmail(userLogin, email);

		return ResponseEntity.ok(new UserEmailDto(user.email().value()));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/{userLogin}/info")
	public ResponseEntity<UserInfoResponseDto> getUserInfo(
			@AuthenticationPrincipal UserLogin requesterLogin,
			@PathVariable String userLogin
	) {
		UserLogin login = new UserLogin(userLogin);
		List<AccountInfo> accounts = accountService.findAllForUser(login)
				.stream()
				.map(AccountMapper::toAccountInfo)
				.toList();

		UserWithRolesView user = userService.findUserWithRoles(requesterLogin, login);

		return ResponseEntity.ok(
				userMapper.toResponse(user, accounts)
		);
	}

	@SecurityRequirement(name = "bearerAuth")
	@PreAuthorize("hasRole('SENIOR_MANAGER')")
	@PostMapping
	public ResponseEntity<UserDetailsResponseDto> createStaffUser(
			@AuthenticationPrincipal UserLogin managerLogin,
			@Valid @RequestBody StaffUserRegistrationDto staffUserRegistrationDto
	) {
		UserWithRolesView staffUser = userService.createStaffUser(
				managerLogin,
				userMapper.toEntity(staffUserRegistrationDto),
				staffUserRegistrationDto.roleName()
		);

		return ResponseEntity.status(HttpStatus.CREATED).body(
				userMapper.toResponse(
						staffUser
				)
		);
	}
}
