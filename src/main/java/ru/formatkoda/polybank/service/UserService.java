package ru.formatkoda.polybank.service;

import org.springframework.stereotype.Service;
import ru.formatkoda.polybank.domain.User;
import ru.formatkoda.polybank.dto.UserRegistrationDto;
import ru.formatkoda.polybank.dto.AuthUserDto;
import ru.formatkoda.polybank.repository.UserRepository;

import java.util.Optional;

@Service
public class UserService {
    private UserRepository repository;

    public AuthUserDto createUser(UserRegistrationDto user) {

    }
}
