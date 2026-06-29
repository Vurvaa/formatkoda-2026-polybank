package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.formatkoda.polybank.domain.User;
import ru.formatkoda.polybank.dto.AuthUserDto;
import ru.formatkoda.polybank.dto.UserRegistrationDto;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final UserService userService;

    public AuthUserDto registerUser(UserRegistrationDto userDto) {
        User user = userService.createUser(userDto);
        String token = jwtService.generateToken(user.login());

        return new AuthUserDto(token);
    }
}