package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.RoleEntity;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.domain.UserLogin;
import ru.formatkoda.polybank.exceptions.BusinessLogicException;
import ru.formatkoda.polybank.repository.RoleRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public UserLogin createUser(UserEntity user) {
        Optional<UserEntity> userOptional = userRepository.findUserByLogin(user.login());
        if (userOptional.isPresent())
            throw new BusinessLogicException("user already exists");

        RoleEntity role = roleRepository
                .findRoleEntityByName("CLIENT")
                .orElseThrow(() -> new ResourceNotFoundException("not found CLIENT role"));

        long userId = userRepository.createUserAndReturnId(user);
        long roleId = roleEntityOptional.get().id();

        userRepository.bindUserWithRole(userId, role.id());

        return user.login();
    }

    public UserEntity findUserByLogin(UserLogin login) {
        return userRepository.findUserByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("not found user with this login"));
    }
}
