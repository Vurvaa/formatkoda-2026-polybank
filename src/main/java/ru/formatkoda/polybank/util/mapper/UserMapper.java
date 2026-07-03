package ru.formatkoda.polybank.util.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.dto.UserRegistrationDto;

@Component
@RequiredArgsConstructor
public class UserMapper {
    private final PasswordEncoder passwordEncoder;

    public UserEntity toEntity(UserRegistrationDto user) {
        String passwordHash = Objects.requireNonNull(
                passwordEncoder.encode(user.password()),
                "encoded password must not be null"
        );

        return new UserEntity(
                null,
                new UserLogin(user.login()),
                user.name(),
                user.lastName(),
                passwordHash,
                OffsetDateTime.now(ZoneOffset.UTC),
                null
        );
    }

    public UserEntity toEntity(UserLoginDto source) {
        return  new UserEntity(
                null,
                source.login(),
                "",
                "",
                passwordEncoder.encode(source.password()),
                null,
                null
        );
    }
}
