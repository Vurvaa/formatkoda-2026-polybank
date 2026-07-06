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

        userRepository.bindUserWithRole(userId, role.id());

        return user.login();
    }

    public UserEntity findUserByLogin(UserLogin login) {
        return userRepository.findUserByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("not found user with this login"));
    }

    @Transactional
    public UserEntity blockUserById(UserLogin userLogin, Long userId) {
        UserEntity blockingUser = userRepository
                .findUserByLogin(userLogin)
                .orElseThrow(() -> new BusinessLogicException("user not found"));
        if (blockingUser.id().equals(userId)) {
            throw new BusinessLogicException("user can not block himself");
        }

        UserEntity potentiallyBlockedUser = userRepository
                .findUserById(userId)
                .orElseThrow(() -> new BusinessLogicException("user not found"));
        if (potentiallyBlockedUser.isBlocked()) {
            return potentiallyBlockedUser;
        }

        return userRepository
                .blockUserById(userId)
                .orElseThrow(() -> new BusinessLogicException("user not blocked"));
    }

    @Transactional
    public UserEntity unBlockUserById(UserLogin userLogin, Long userId) {
        UserEntity unBlockingUser = userRepository
                .findUserByLogin(userLogin)
                .orElseThrow(() -> new BusinessLogicException("user not found"));
        if (unBlockingUser.id().equals(userId)) {
            throw new BusinessLogicException("user can not unblock himself");
        }

        UserEntity potentiallyUnBlockedUser = userRepository
                .findUserById(userId)
                .orElseThrow(() -> new BusinessLogicException("user not found"));
        if (!potentiallyUnBlockedUser.isBlocked()) {
            return potentiallyUnBlockedUser;
        }

        return userRepository
                .unBlockUserById(userId)
                .orElseThrow(() -> new BusinessLogicException("user not unblocked"));
    }
}
