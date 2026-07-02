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

    public UserEntity toEntity(UserRegistrationDto source) {
        return  new UserEntity(
                null,
                source.login(),
                source.name(),
                source.lastName(),
                passwordEncoder.encode(source.password()),
                null,
                null
        );
    }
}
