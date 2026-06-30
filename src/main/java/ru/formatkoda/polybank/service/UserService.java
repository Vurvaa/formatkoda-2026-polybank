package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.formatkoda.polybank.domain.Authority;
import ru.formatkoda.polybank.domain.User;
import ru.formatkoda.polybank.dto.UserRegistrationDto;
import ru.formatkoda.polybank.exceptions.UserAlreadyExistsException;
import ru.formatkoda.polybank.repository.AuthorityRepository;
import ru.formatkoda.polybank.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;

    public String createUser(UserRegistrationDto user) {
        Optional<User> userOptional = userRepository.findUserByLogin(user.login());
        if (!userOptional.isEmpty()) {
            throw new UserAlreadyExistsException("this login already exist");
        }

        Optional<Authority> authorityOptional = Optional.of(authorityRepository.findByAuthority("ROLE_USER"));
        if (authorityOptional.isEmpty()) {
            throw new RuntimeException("authority not found");
        }

        return userRepository.insertUserAndReturnLogin(user);
    }
}
