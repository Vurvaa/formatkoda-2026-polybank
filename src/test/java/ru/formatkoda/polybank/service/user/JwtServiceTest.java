package ru.formatkoda.polybank.service.user;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.formatkoda.polybank.service.JwtService;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secret",
                getBaseSecret("test-secret-key-test-secret-key-32"));
        ReflectionTestUtils.setField(jwtService, "expirationMinutes", 60L);
    }

    @Test
    void generateTokenShouldGenerateNotBlankToken() {
        String token = jwtService.generateToken("value");

        assertThat(token).isNotBlank();
    }

    @Test
    void extractUserLoginFromTokenShouldReturnUserLoginFromValidToken() {
        String token = jwtService.generateToken("value");

        String result = jwtService.extractUserLoginFromToken(token);

        assertThat(result).isEqualTo("value");
    }

    @Test
    void generateTokenShouldThrowExceptionWhenTokenSignedWithAnotherSecret() {
        JwtService anotherJwtService = new JwtService();

        ReflectionTestUtils.setField(anotherJwtService, "secret",
                getBaseSecret("another-secret-key-another-key-32"));
        ReflectionTestUtils.setField(anotherJwtService, "expirationMinutes", 60L);

        String token = anotherJwtService.generateToken("value");

        assertThrows(JwtException.class,
                () -> jwtService.extractUserLoginFromToken(token));
    }

    @Test
    void generateTokenShouldThrowExpiredJwtExceptionWhenTokenExpired() {
        ReflectionTestUtils.setField(jwtService, "expirationMinutes", -1L);

        String token = jwtService.generateToken("value");

        assertThrows(ExpiredJwtException.class,
                () -> jwtService.extractUserLoginFromToken(token));
    }

    private String getBaseSecret(String rawSecret) {
        return Base64.getEncoder()
                .encodeToString(rawSecret.getBytes());
    }
}