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
            throw new BusinessLogicException("login already exists");

        Optional<RoleEntity> roleEntityOptional = roleRepository.findRoleEntityByName("CLIENT");
        if (roleEntityOptional.isEmpty())
            throw new BusinessLogicException("not found CLIENT role");

        long userId = userRepository.createUserAndReturnId(user);
        long roleId = roleEntityOptional.get().id();

        userRepository.bindUserWithRole(userId, roleId);

        return new UserLogin(user.login());
    }

    public UserEntity findUserByLogin(String login) {
        return userRepository.findUserByLogin(login)
                .orElseThrow(() -> new BusinessLogicException("not found user with this login"));
    }
}
