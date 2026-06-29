package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.formatkoda.polybank.domain.Authority;
import ru.formatkoda.polybank.domain.User;
import ru.formatkoda.polybank.dto.UserRegistrationDto;
import ru.formatkoda.polybank.exceptions.UserAlreadyExistsException;
import ru.formatkoda.polybank.repository.AuthorityRepository;
import ru.formatkoda.polybank.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final  UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public User createUser(UserRegistrationDto user) {
        Optional<User> userOptional = Optional.of(userRepository.findUserByLogin(user.login()));
        if (!userOptional.isEmpty()) {
            throw new UserAlreadyExistsException("this login already exist");
        }

        Optional<Authority> authorityOptional = Optional.of(authorityRepository.findByAuthority("USER_ROLE"));
        if (authorityOptional.isEmpty()) {
            throw new RuntimeException("authority not found");
        }

        User userEntity = new User(
                null,
                user.login(),
                user.name(),
                user.lastName(),
                user.password(passwordEncoder.encode(user.password())),
                OffsetDateTime.now(),
                null
        );

        return userEntity;
    }
}
