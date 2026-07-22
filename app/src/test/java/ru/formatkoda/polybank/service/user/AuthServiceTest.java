package ru.formatkoda.polybank.service.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.auth.JwtToken;
import ru.formatkoda.polybank.service.AuthService;
import ru.formatkoda.polybank.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

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