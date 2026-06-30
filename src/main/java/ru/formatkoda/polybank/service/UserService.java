package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.RoleEntity;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.domain.UserLogin;
import ru.formatkoda.polybank.domain.exceptions.RoleNotFoundException;
import ru.formatkoda.polybank.domain.exceptions.UserNotFoundException;
import ru.formatkoda.polybank.domain.exceptions.UserAlreadyExistsException;
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
            throw new UserAlreadyExistsException();

        Optional<RoleEntity> roleEntityOptional = roleRepository.findRoleEntityByName("CLIENT");
        if (roleEntityOptional.isEmpty())
            throw new RoleNotFoundException();

        long userId = userRepository.createUserAndReturnId(user);
        long roleId = roleEntityOptional.get().id();

        userRepository.bindUserWithRole(userId, roleId);

        return new UserLogin(user.login());
    }

    public UserEntity findUserByLogin(String login) {
        return userRepository.findUserByLogin(login).orElseThrow(UserNotFoundException::new);
    }
}
