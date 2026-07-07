package ru.formatkoda.polybank.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.repository.RoleRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.util.mapper.UserMapper;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

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
    public UserLogin createUser(@NonNull UserEntity user) {
        Optional<UserEntity> userOptional = userRepository.findUserByLogin(user.login());
        if (userOptional.isPresent())
            throw new BusinessLogicException("user already exists");

        long roleId = findRoleIdOrThrow("CLIENT");

        Long userId = userRepository.createUserAndReturnId(user)
                .orElseThrow(() -> new BusinessLogicException("unable to create user"));

        userRepository.bindUserWithRole(userId, roleId);

        return user.login();
    }

    public UserEntity findUserByLogin(@NonNull UserLogin login) {
        return userRepository.findUserByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("not found user with this login"));
    }

    public UserEntity findNotBlockedUserByLogin(@NonNull UserLogin login) {
        UserEntity user = findUserByLogin(login);
        if (user.isBlocked()) {
            throw new BusinessLogicException(String.format("user %s is blocked", login.value()));
        }

        return user;
    }

    @Transactional
    public UserWithRolesView blockUserByLogin(
            @NonNull UserLogin managerLogin,
            @NonNull UserLogin userLogin
    ) {
        UserEntity manager = findUserByLogin(managerLogin);
        UserEntity user = findUserByLogin(userLogin);
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
    public UserWithRolesView unBlockUserByLogin(
            @NonNull UserLogin managerLogin,
            @NonNull UserLogin userLogin) {
        UserEntity manager = findUserByLogin(managerLogin);
        UserEntity user = findUserByLogin(userLogin);
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

    @Transactional
    public UserWithRolesView removeManagerRoleByLogin(
            @NonNull UserLogin managerLogin,
            @NonNull UserLogin userLogin
    ) {
        findNotBlockedUserByLogin(managerLogin);

        UserEntity user = findUserByLogin(userLogin);
        List<String> userRoles = findAllUserRoles(user);
        if (!userRoles.contains("MANAGER")) {
            throw new BusinessLogicException("user has not manager role");
        }

        long managerRoleId = findRoleIdOrThrow("MANAGER");
        if (!userRepository.removeUserRole(user.id(), managerRoleId)) {
            throw new BusinessLogicException("user manager role not removed");
        }

        UserEntity updatedUser = findUserByLogin(userLogin);
        List<String> updatedUserRoles = findAllUserRoles(updatedUser);

        return userMapper.toUserWithRolesView(
                updatedUser,
                updatedUserRoles
        );
    }

    public List<String> findAllUserRoles(@NonNull UserEntity user) {
        return userRepository.findAllUserRoles(user);
    }

    public PageResult<UserWithRolesView> findAllUsersWithRoles(@NonNull PageRequest pageRequest) {
        List<UserWithRolesView> users = userRepository
                .findAllUsers(pageRequest).stream()
                .map(u -> userMapper
                        .toUserWithRolesView(u, userRepository.findAllUserRoles(u)))
                .toList();

        return new PageResult<>(
                users,
                pageRequest.page(),
                pageRequest.size(),
                users.size()
        );
    }

    private long findRoleIdOrThrow(@NonNull String roleName) {
        return roleRepository
                .findRoleEntityByName(roleName)
                .orElseThrow(
                        () -> new ResourceNotFoundException(String.format("role with name %s not found", roleName)
                        )
                ).id();
    }
}
