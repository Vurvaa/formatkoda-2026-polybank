package ru.formatkoda.polybank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.domain.UserLogin;
import ru.formatkoda.polybank.domain.auth.JwtToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerUserShouldCreateUserGenerateTokenAndReturnJwtToken() {
        UserEntity user = mock(UserEntity.class);

        UserLogin userLogin = new UserLogin("login");
        String generatedToken = "test.jwt.token";

        when(userService.createUser(user)).thenReturn(userLogin);
        when(jwtService.generateToken("login")).thenReturn(generatedToken);

        JwtToken result = authService.registerUser(user);

        assertThat(result.token()).isEqualTo(generatedToken);

        verify(userService).createUser(user);
        verify(jwtService).generateToken("login");
    }
}