package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.user.RoleEntity;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.repository.RoleRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.util.mapper.UserMapper;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

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
    public UserWithRolesView blockUserByLogin(UserLogin managerLogin, UserLogin userLogin) {
        UserEntity manager = userRepository
                .findUserByLogin(managerLogin)
                .orElseThrow(() -> new ResourceNotFoundException("manager not found"));
        UserEntity user = userRepository
                .findUserByLogin(userLogin)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));
        if (user.id().equals(manager.id())) {
            throw new BusinessLogicException("manager can not block himself");
        }

        List<String> roles = findAllUserRoles(user);

        if (user.isBlocked()) {
            return userMapper.toUserWithRolesView(user, roles);
        }

        return userMapper.toUserWithRolesView(
                userRepository
                .blockUserById(user.id())
                .orElseThrow(() -> new BusinessLogicException("user not blocked")),
                roles
        );
    }

    @Transactional
    public UserWithRolesView unBlockUserByLogin(UserLogin managerLogin, UserLogin userLogin) {
        UserEntity manager = userRepository
                .findUserByLogin(managerLogin)
                .orElseThrow(() -> new ResourceNotFoundException("manager not found"));
        UserEntity user = userRepository
                .findUserByLogin(userLogin)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));
        if (user.id().equals(manager.id())) {
            throw new BusinessLogicException("manager can not unblock himself");
        }

        List<String> roles = findAllUserRoles(user);

        if (!user.isBlocked()) {
            return userMapper.toUserWithRolesView(user, roles);
        }

        return userMapper.toUserWithRolesView(
                userRepository
                        .unBlockUserById(user.id())
                        .orElseThrow(() -> new BusinessLogicException("user not unblocked")),
                roles
        );
    }

    public List<String> findAllUserRoles(UserEntity user) {
        return userRepository.findAllUserRoles(user);
    }
}
