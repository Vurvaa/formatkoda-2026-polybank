package ru.formatkoda.polybank.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.formatkoda.polybank.dto.UserRegistrationDto;
import ru.formatkoda.polybank.dto.AuthUserDto;
import ru.formatkoda.polybank.service.UserService;


@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/sign-up")
    private ResponseEntity<AuthUserDto> registrationUser(UserRegistrationDto user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(user));
    }
}
