package ru.formatkoda.polybank.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.formatkoda.polybank.domain.auth.JwtToken;
import ru.formatkoda.polybank.dto.user.UserRegistrationDto;
import ru.formatkoda.polybank.dto.user.AuthUserDto;
import ru.formatkoda.polybank.service.AuthService;
import ru.formatkoda.polybank.util.mapper.UserMapper;

@RestController
@RequestMapping(path = "/user")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;
    private final UserMapper userMapper;

    @PostMapping("/sign-up")
    public ResponseEntity<AuthUserDto> registrationUser(@RequestBody @Valid UserRegistrationDto user) {
        JwtToken token = authService.registerUser(userMapper.toEntity(user));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthUserDto(token.token()));
    }
}
