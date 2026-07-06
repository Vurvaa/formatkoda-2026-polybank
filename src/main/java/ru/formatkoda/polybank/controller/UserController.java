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
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.dto.user.UserLoginDto;
import ru.formatkoda.polybank.dto.user.UserRegistrationDto;
import ru.formatkoda.polybank.dto.user.AuthUserDto;
import ru.formatkoda.polybank.service.AuthService;
import ru.formatkoda.polybank.service.UserService;
import ru.formatkoda.polybank.util.mapper.UserMapper;
import ru.formatkoda.polybank.dto.user.UserDetailsResponseDto;

@RestController
@RequestMapping(path = "/user")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;
    private final UserMapper userMapper;

    private final UserService userService;

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
    @PostMapping("/{userLogin}/block")
    public ResponseEntity<UserDetailsResponseDto> blockUser(
            @AuthenticationPrincipal UserLogin managerLogin,
            @Valid @PathVariable UserLogin userLogin
    ) {
        UserWithRolesView user = userService.blockUserById(managerLogin, userLogin);

        return ResponseEntity.ok(userMapper.toUserDetailsResponseDto(user));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SENIOR_MANAGER')")
    @PostMapping("/{userLogin}/unblock")
    public ResponseEntity<UserDetailsResponseDto> unBlockUser(
            @AuthenticationPrincipal UserLogin managerLogin,
            @Valid @PathVariable UserLogin userLogin
    ) {
        UserWithRolesView user = userService.unBlockUserById(managerLogin, userLogin);

        return ResponseEntity.ok(userMapper.toUserDetailsResponseDto(user));
    }
}
