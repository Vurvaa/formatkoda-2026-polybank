package ru.formatkoda.polybank.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.user.UserEmail;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.messaging.publisher.UserEventPublisher;
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

    private static final String CLIENT_ROLE = "CLIENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final UserEventPublisher userEventPublisher;

    @Transactional
    public UserLogin createUser(@NonNull UserEntity user) {
        Optional<UserEntity> userOptional = userRepository.findUserByLogin(user.login());

        boolean emailExists = userRepository.existsUserByEmail(user.email());

        if (userOptional.isPresent() || emailExists)
            throw new BusinessLogicException("user already exists");


        long roleId = findRoleIdOrThrow(CLIENT_ROLE);

        Long userId = userRepository.createUserAndReturnId(user)
                .orElseThrow(() -> new BusinessLogicException("unable to create user"));

        userRepository.bindUserWithRole(userId, roleId);

        UserEntity registered = new UserEntity(
                userId,
                user.login(),
                user.email(),
                user.name(),
                user.lastName(),
                user.passwordHash(),
                user.createdAt(),
                user.blockedAt()
        );

        userEventPublisher.publishUserRegistered(registered, CLIENT_ROLE);

        return user.login();
    }

    public boolean hasRole(UserLogin login, String roleName) {
        UserEntity user = findUserByLogin(login);
        List<String> roles = findAllUserRoles(user);

        return roles.contains(roleName);
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
    public UserWithRolesView addUserRole(
            @NonNull UserLogin managerLogin,
            @NonNull UserLogin userLogin,
            @NonNull String roleName
    ) {
        UserEntity manager = findNotBlockedUserByLogin(managerLogin);
        UserEntity user = findUserByLogin(userLogin);

        if (user.id().equals(manager.id())) {
            throw new BusinessLogicException("user can not add role himself");
        }

        List<String> userRoles = findAllUserRoles(user);
        if (userRoles.contains(roleName)) {
            throw new BusinessLogicException(
                    String.format(
                            "user already has %s role",
                            roleName
                    )
            );
        }

        long userId = user.id();
        long roleId = findRoleIdOrThrow(roleName);
        userRepository.bindUserWithRole(userId, roleId);

        UserEntity updatedUser = findUserByLogin(userLogin);
        List<String> updatedRoles = findAllUserRoles(updatedUser);

        return userMapper.toUserWithRolesView(updatedUser, updatedRoles);
    }

    @Transactional
    public UserEntity addUserEmail(
            @NonNull UserLogin userLogin,
            @NonNull UserEmail userEmail) {
        Optional<UserEntity> userOptional = userRepository.findUserByEmail(userEmail);
        if (userOptional.isPresent())
            throw new BusinessLogicException("this email already exist");

        userOptional = userRepository.changeUserEmail(userLogin, userEmail);
        return userOptional.orElseThrow(
                () -> new BusinessLogicException("email not changed")
        );
    }

    @Transactional
    public UserWithRolesView removeUserRole(
            @NonNull UserLogin managerLogin,
            @NonNull UserLogin userLogin,
            @NonNull String roleName
    ) {
        UserEntity manager = findNotBlockedUserByLogin(managerLogin);
        UserEntity user = findUserByLogin(userLogin);

        if  (user.id().equals(manager.id())) {
            throw new BusinessLogicException("user can not remove himself");
        }
        List<String> userRoles = findAllUserRoles(user);

        if (!userRoles.contains(roleName)) {
            throw new BusinessLogicException(
                    String.format(
                            "user has not %s role",
                            roleName
                    )
            );
        } else if (userRoles.size() < 2) {
            throw new BusinessLogicException("user has only one role, use block instead");
        }

        long roleId = findRoleIdOrThrow(roleName);
        if (!userRepository.removeUserRole(user.id(), roleId)) {
            throw new BusinessLogicException(
                    String.format(
                            "user %s role not removed",
                            roleName
                    )
            );
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

    public UserWithRolesView findUserWithRoles(
            @NonNull UserLogin requesterLogin,
            @NonNull UserLogin targetLogin
    ) {
        UserEntity targetUser = findUserByLogin(targetLogin);
        List<String> targetRoles = findAllUserRoles(targetUser);

        if (requesterLogin.equals(targetLogin)) {
            return userMapper.toUserWithRolesView(targetUser, targetRoles);
        }

        UserEntity requester = findUserByLogin(requesterLogin);
        List<String> requesterRoles = findAllUserRoles(requester);

        if (!requesterRoles.contains("MANAGER") && !requesterRoles.contains("SENIOR_MANAGER")) {
            throw new BusinessLogicException("not allowed to view another user's info");
        }

        return userMapper.toUserWithRolesView(targetUser, targetRoles);
    }

    @Transactional
    public UserWithRolesView createStaffUser(
            @NonNull UserLogin managerLogin,
            @NonNull UserEntity staffUser,
            @NonNull String roleName
    ) {
        findNotBlockedUserByLogin(managerLogin);

        if (roleName.equals(CLIENT_ROLE)) {
            throw new BusinessLogicException("only staff user can be created by manager");
        } else if (userRepository.findUserByLogin(staffUser.login()).isPresent()) {
            throw new BusinessLogicException("user with given login already exists");
        }

        long roleId = findRoleIdOrThrow(roleName);

        UserEntity createdUser = userRepository.createUser(
                staffUser
        ).orElseThrow(() -> new BusinessLogicException("user not created"));
        userRepository.bindUserWithRole(createdUser.id(), roleId);

        userEventPublisher.publishUserRegistered(createdUser, roleName);

        return userMapper.toUserWithRolesView(
                createdUser,
                findAllUserRoles(createdUser)
        );
    }

    public PageResult<UserWithRolesView> findAllUsersWithRoles(@NonNull PageRequest pageRequest) {
        List<UserEntity> users = userRepository.findAllUsers(pageRequest);
        Long total = userRepository.countAll();

        List<UserWithRolesView> usersViews = users.stream()
                .map(u -> userMapper.toUserWithRolesView(
                        u, userRepository.findAllUserRoles(u))
                )
                .toList();

        return new PageResult<>(
                usersViews,
                pageRequest.page(),
                pageRequest.size(),
                total
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
