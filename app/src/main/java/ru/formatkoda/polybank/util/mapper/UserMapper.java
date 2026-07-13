package ru.formatkoda.polybank.util.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.account.AccountInfo;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.dto.user.StaffUserRegistrationDto;
import ru.formatkoda.polybank.dto.user.UserDetailsResponseDto;
import ru.formatkoda.polybank.dto.user.UserInfoResponseDto;
import ru.formatkoda.polybank.dto.user.UserLoginDto;
import ru.formatkoda.polybank.dto.user.UserRegistrationDto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

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

    public UserEntity toEntity(UserLoginDto user) {
        return  new UserEntity(
                null,
                new UserLogin(user.login()),
                "",
                "",
                user.password(),
                OffsetDateTime.now(ZoneOffset.UTC),
                null
        );
    }

    public UserEntity toEntity(StaffUserRegistrationDto user) {
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

    public UserDetailsResponseDto toResponse(UserWithRolesView user) {
        return new UserDetailsResponseDto(
                user.login(),
                user.name(),
                user.lastName(),
                user.roles(),
                user.createdAt(),
                user.blockedAt()
        );
    }

    public UserInfoResponseDto toResponse(UserWithRolesView user, List<AccountInfo> accounts) {
        return new UserInfoResponseDto(
                user.login(),
                user.name(),
                user.lastName(),
                user.roles(),
                accounts,
                user.createdAt(),
                user.blockedAt()
        );
    }

    public UserWithRolesView toUserWithRolesView(UserEntity user, List<String> roles) {
        return new UserWithRolesView(
                user.id(),
                user.login(),
                user.name(),
                user.lastName(),
                roles,
                user.createdAt(),
                user.blockedAt()
        );
    }
}
