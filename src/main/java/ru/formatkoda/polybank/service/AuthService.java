package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.domain.UserLogin;
import ru.formatkoda.polybank.domain.auth.JwtToken;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final UserService userService;

    public JwtToken registerUser(UserEntity user) {
        UserLogin userLogin = userService.createUser(user);
        String token = jwtService.generateToken(userLogin.login());

        return new JwtToken(token);
    }
}