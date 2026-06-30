package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.formatkoda.polybank.dto.AuthUserDto;
import ru.formatkoda.polybank.dto.UserRegistrationDto;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final UserService userService;

    public AuthUserDto registerUser(UserRegistrationDto userDto) {
        String login = userService.createUser(userDto);
        String token = jwtService.generateToken(login);

        return new AuthUserDto(token);
    }
}