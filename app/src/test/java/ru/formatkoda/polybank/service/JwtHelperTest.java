package ru.formatkoda.polybank.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.formatkoda.polybank.security.JwtHelper;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtHelperTest {
    private JwtHelper jwtHelper;

    @BeforeEach
    void setUp() {
        jwtHelper = new JwtHelper();

        ReflectionTestUtils.setField(jwtHelper, "secret",
                getBaseSecret("test-secret-key-test-secret-key-32"));
        ReflectionTestUtils.setField(jwtHelper, "expirationMinutes", 60L);
    }

    @Test
    void generateTokenShouldCreateNotBlankToken() {
        String token = jwtHelper.createToken("login");

        assertThat(token).isNotBlank();
    }

    @Test
    void extractUserLoginFromTokenShouldReturnUserLoginFromValidToken() {
        String token = jwtHelper.createToken("login");

        String result = jwtHelper.extractUserLoginFromToken(token);

        assertThat(result).isEqualTo("login");
    }

    @Test
    void createTokenShouldThrowExceptionWhenTokenSignedWithAnotherSecret() {
        JwtHelper anotherJwtHelper = new JwtHelper();

        ReflectionTestUtils.setField(anotherJwtHelper, "secret",
                getBaseSecret("another-secret-key-another-key-32"));
        ReflectionTestUtils.setField(anotherJwtHelper, "expirationMinutes", 60L);

        String token = anotherJwtHelper.createToken("login");

        assertThrows(JwtException.class,
                () -> jwtHelper.extractUserLoginFromToken(token));
    }

    @Test
    void createTokenShouldThrowExpiredJwtExceptionWhenTokenExpired() {
        ReflectionTestUtils.setField(jwtHelper, "expirationMinutes", -1L);

        String token = jwtHelper.createToken("login");

        assertThrows(ExpiredJwtException.class,
                () -> jwtHelper.extractUserLoginFromToken(token));
    }

    private String getBaseSecret(String rawSecret) {
        return Base64.getEncoder()
                .encodeToString(rawSecret.getBytes());
    }
}