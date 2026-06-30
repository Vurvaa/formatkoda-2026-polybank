package ru.formatkoda.polybank.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.formatkoda.polybank.dto.UserRegistrationDto;
import ru.formatkoda.polybank.dto.AuthUserDto;
import ru.formatkoda.polybank.service.AuthService;

@RestController
@RequestMapping(path = "/user", version = "v1")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<AuthUserDto> registrationUser(@RequestBody @Valid UserRegistrationDto user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(user));
    }
}
