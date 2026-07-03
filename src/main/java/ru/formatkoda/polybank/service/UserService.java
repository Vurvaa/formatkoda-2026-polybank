package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.user.RoleEntity;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.repository.RoleRepository;
import ru.formatkoda.polybank.repository.UserRepository;

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
                .orElseThrow(() -> new BusinessLogicException("not found CLIENT role"));

        long userId = userRepository.createUserAndReturnId(user);

        userRepository.bindUserWithRole(userId, role.id());

        return user.login();
    }

    public UserEntity findUserByLogin(UserLogin login) {
        return userRepository.findUserByLogin(login)
                .orElseThrow(() -> new BusinessLogicException("not found user with this login"));
    }
}
