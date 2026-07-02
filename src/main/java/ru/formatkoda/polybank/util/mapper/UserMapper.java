package ru.formatkoda.polybank.util.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.dto.user.UserRegistrationDto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class UserMapper {
    private final PasswordEncoder passwordEncoder;

    public UserEntity toEntity(UserRegistrationDto user) {
        return  new UserEntity(
                null,
                new UserLogin(user.login()),
                user.name(),
                user.lastName(),
                passwordEncoder.encode(user.password()),
                OffsetDateTime.now(ZoneOffset.UTC),
                null
        );
    }
}
