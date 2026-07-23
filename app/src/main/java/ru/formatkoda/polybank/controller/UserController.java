package ru.formatkoda.polybank.controller;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.formatkoda.polybank.config.CurrentUserLogin;
import ru.formatkoda.polybank.domain.account.AccountInfo;
import ru.formatkoda.polybank.domain.user.UserEmail;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.dto.user.ChangedUserPasswordDto;
import ru.formatkoda.polybank.dto.user.StaffUserRegistrationDto;
import ru.formatkoda.polybank.dto.user.UserDetailsResponseDto;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.dto.user.UserEmailDto;
import ru.formatkoda.polybank.dto.user.UserRegistrationDto;
import ru.formatkoda.polybank.dto.user.UserInfoResponseDto;
import ru.formatkoda.polybank.ratelimiting.RateLimit;
import ru.formatkoda.polybank.service.AccountService;
import ru.formatkoda.polybank.service.UserService;
import ru.formatkoda.polybank.util.mapper.AccountMapper;
import ru.formatkoda.polybank.util.mapper.UserMapper;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResponse;
import ru.formatkoda.polybank.util.pagination.PageResult;

import java.util.List;

@RestController
@RequestMapping(path = "/user")
@RateLimit(requests = 100)
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AccountService accountService;
    private final UserMapper userMapper;

    @GetMapping("/demo")
    public String demo() {
        return "DEMO_ENDPOINT";
    }

    @PostMapping("/sign-up")
    public ResponseEntity<Void> registrationUser(@RequestBody @Valid UserRegistrationDto user) {
        userService.createUser(userMapper.toEntity(user));
        return ResponseEntity.ok().build();
    }

    @SecurityRequirement(name = "oauth2")
    @PreAuthorize("hasRole('SENIOR_MANAGER')")
    @PutMapping("/{userLogin}/block")
    public ResponseEntity<UserDetailsResponseDto> blockUser(
            @CurrentUserLogin UserLogin managerLogin,
            @Valid @PathVariable String userLogin
    ) {
        UserWithRolesView user = userService.blockUserByLogin(
                managerLogin,
                new UserLogin(userLogin)
        );

        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @SecurityRequirement(name = "oauth2")
    @PreAuthorize("hasRole('SENIOR_MANAGER')")
    @PutMapping("/{userLogin}/unblock")
    public ResponseEntity<UserDetailsResponseDto> unBlockUser(
            @CurrentUserLogin UserLogin managerLogin,
            @Valid @PathVariable String userLogin
    ) {
        UserWithRolesView user = userService.unBlockUserByLogin(
                managerLogin,
                new UserLogin(userLogin)
        );

        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @SecurityRequirement(name = "oauth2")
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

    @SecurityRequirement(name = "oauth2")
    @PreAuthorize("hasRole('SENIOR_MANAGER')")
    @DeleteMapping("/{userLogin}/roles/{roleName}")
    public ResponseEntity<UserDetailsResponseDto> removeUserRole(
            @CurrentUserLogin UserLogin managerLogin,
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

    @SecurityRequirement(name = "oauth2")
    @PreAuthorize("hasRole('SENIOR_MANAGER')")
    @PutMapping("/{userLogin}/roles/{roleName}")
    public ResponseEntity<UserDetailsResponseDto> addUserRole(
            @CurrentUserLogin UserLogin managerLogin,
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

    @SecurityRequirement(name = "oauth2")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/mail")
    public ResponseEntity<Void> changeUserEmail(
            @CurrentUserLogin UserLogin userLogin,
            @RequestBody @Valid UserEmailDto userEmail
    ) {
        UserEmail email = new UserEmail(userEmail.value());
        userService.changeUserEmail(userLogin, email);

        return ResponseEntity.ok().build();
    }

    @SecurityRequirement(name = "oauth2")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/password")
    public ResponseEntity<Void> changeUserPassword(
            @RequestBody @Valid ChangedUserPasswordDto changedUserPassword
    ) {

        userService.changeUserPassword(
                userMapper.toUserPasswordChangedView(changedUserPassword)
        );

        return ResponseEntity.ok().build();
    }


    @SecurityRequirement(name = "oauth2")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{userLogin}/info")
    public ResponseEntity<UserInfoResponseDto> getUserInfo(
            @CurrentUserLogin UserLogin requesterLogin,
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

    @SecurityRequirement(name = "oauth2")
    @PreAuthorize("hasRole('SENIOR_MANAGER')")
    @PostMapping
    public ResponseEntity<UserDetailsResponseDto> createStaffUser(
            @CurrentUserLogin UserLogin managerLogin,
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
