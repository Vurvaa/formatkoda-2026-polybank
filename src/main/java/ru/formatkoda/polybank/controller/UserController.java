package ru.formatkoda.polybank.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.formatkoda.polybank.domain.auth.JwtToken;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
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

    @PreAuthorize("hasAuthority('SENIOR_MANAGER')")
    @PostMapping("/{userId}/block")
    public ResponseEntity<UserDetailsResponseDto> blockUser(
            @AuthenticationPrincipal UserLogin userLogin,
            @PathVariable @Min(1) Long userId
    ) {
        UserEntity user = userService.blockUserById(userLogin, userId);

        return ResponseEntity.ok(userMapper.toUserDetailsResponseDto(user));
    }

    @PreAuthorize("hasAuthority('SENIOR_MANAGER')")
    @PostMapping("/{userId}/block")
    public ResponseEntity<UserDetailsResponseDto> unBlockUser(
            @AuthenticationPrincipal UserLogin userLogin,
            @PathVariable @Min(1) Long userId
    ) {
        UserEntity user = userService.unBlockUserById(userLogin, userId);

        return ResponseEntity.ok(userMapper.toUserDetailsResponseDto(user));
    }
}
