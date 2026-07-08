package ru.formatkoda.polybank.controller;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.formatkoda.polybank.domain.auth.JwtToken;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.dto.user.UserDetailsResponseDto;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.dto.user.UserLoginDto;
import ru.formatkoda.polybank.dto.user.UserRegistrationDto;
import ru.formatkoda.polybank.dto.user.AuthUserDto;
import ru.formatkoda.polybank.service.AuthService;
import ru.formatkoda.polybank.service.UserService;
import ru.formatkoda.polybank.util.mapper.UserMapper;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResponse;
import ru.formatkoda.polybank.util.pagination.PageResult;

@RestController
@RequestMapping(path = "/user")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;
    private final UserService userService;
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
}
