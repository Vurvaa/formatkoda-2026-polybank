package ru.formatkoda.polybank.service.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.user.RoleEntity;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.repository.RoleRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.service.UserService;
import ru.formatkoda.polybank.util.mapper.UserMapper;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static ru.formatkoda.polybank.testutil.TestData.CREATED_AT;
import static ru.formatkoda.polybank.testutil.TestData.SENIOR_MANAGER_LOGIN;
import static ru.formatkoda.polybank.testutil.TestData.user;
import static ru.formatkoda.polybank.testutil.TestData.userManager;

@ExtendWith(MockitoExtension.class)
class UserServiceManagerOperationsTest {
    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    UserMapper userMapper;

    @InjectMocks
    UserService userService;

    @Test
    void shouldBlockAccountUser() {
        UserEntity user = user();
        UserEntity userManager = userManager();

        UserLogin userLogin = user.login();
        UserLogin managerLogin = userManager.login();

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));

        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.of(user));

        when(userRepository.findAllUserRoles(user)).thenReturn(List.of("CLIENT"));

        UserEntity blockedUser = mock(UserEntity.class);

        UserWithRolesView blockedUserWithRole = mock(UserWithRolesView.class);

        when(userRepository.blockUserById(user.id())).thenReturn(Optional.of(blockedUser));
        when(userMapper.toUserWithRolesView(blockedUser, List.of("CLIENT"))).thenReturn(blockedUserWithRole);

        UserWithRolesView checkUserWithRolesView = userService.blockUserByLogin(managerLogin, userLogin);

        Assertions.assertEquals(blockedUserWithRole, checkUserWithRolesView);

        verify(userMapper, times(1)).toUserWithRolesView(blockedUser, List.of("CLIENT"));
    }

    @Test
    void shouldNotBlockUnexistingUser() {
        UserEntity blockedUser = mock(UserEntity.class);
        when(blockedUser.id()).thenReturn((long) 35);
        when(blockedUser.login()).thenReturn(new UserLogin("blockedUserLogin"));

        UserEntity userManager = userManager();

        UserLogin blockedUserLogin = blockedUser.login();
        UserLogin managerLogin = userManager.login();

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));

        when(userRepository.findUserByLogin(blockedUser.login()))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> userService.blockUserByLogin(managerLogin, blockedUserLogin));

        verify(userMapper, times(0)).toUserWithRolesView(blockedUser, List.of("CLIENT"));
        verify(userRepository, times(0)).blockUserById(blockedUser.id());
    }

    @Test
    void shouldNotBlockAlreadyBlockedUser() {
        UserEntity blockedUser = mock(UserEntity.class);
        when(blockedUser.isBlocked()).thenReturn(true);
        when(blockedUser.id()).thenReturn((long) 35);
        when(blockedUser.login()).thenReturn(new UserLogin("blockedUserLogin"));

        UserEntity userManager = userManager();

        UserLogin blockedUserLogin = blockedUser.login();
        UserLogin managerLogin = userManager.login();

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));

        when(userRepository.findUserByLogin(blockedUser.login()))
                .thenReturn(Optional.of(blockedUser));

        when(userRepository.findAllUserRoles(blockedUser)).thenReturn(List.of("CLIENT"));

        UserWithRolesView blockedUserWithRole = mock(UserWithRolesView.class);

        when(userMapper.toUserWithRolesView(blockedUser, List.of("CLIENT"))).thenReturn(blockedUserWithRole);

        UserWithRolesView checkUserWithRolesView = userService.blockUserByLogin(managerLogin, blockedUserLogin);

        Assertions.assertEquals(blockedUserWithRole, checkUserWithRolesView);

        verify(userMapper, times(1)).toUserWithRolesView(blockedUser, List.of("CLIENT"));
        verify(userRepository, times(0)).blockUserById(blockedUser.id());
    }

    @Test
    void shouldUnblockAccountUser() {
        UserEntity blockedUser = mock(UserEntity.class);
        when(blockedUser.isBlocked()).thenReturn(true);
        when(blockedUser.login()).thenReturn(new UserLogin("blockedUserLogin"));

        UserEntity userManager = userManager();

        UserLogin blockedUserLogin = blockedUser.login();
        UserLogin managerLogin = userManager.login();

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));

        when(userRepository.findUserByLogin(blockedUser.login()))
                .thenReturn(Optional.of(blockedUser));

        when(userRepository.findAllUserRoles(blockedUser)).thenReturn(List.of("CLIENT"));

        UserEntity unblockedUser = mock(UserEntity.class);
        UserWithRolesView unblockedUserWithRole = mock(UserWithRolesView.class);

        when(userRepository.unBlockUserById(blockedUser.id())).thenReturn(Optional.of(unblockedUser));
        when(userMapper.toUserWithRolesView(unblockedUser, List.of("CLIENT"))).thenReturn(unblockedUserWithRole);

        UserWithRolesView checkUserWithRolesView = userService.unBlockUserByLogin(managerLogin, blockedUserLogin);

        Assertions.assertEquals(unblockedUserWithRole, checkUserWithRolesView);

        verify(userMapper, times(1)).toUserWithRolesView(unblockedUser, List.of("CLIENT"));
    }

    @Test
    void shouldNotUnBlockUnexistingUser() {
        UserEntity blockedUser = mock(UserEntity.class);
        when(blockedUser.id()).thenReturn((long) 35);
        when(blockedUser.login()).thenReturn(new UserLogin("blockedUserLogin"));

        UserEntity userManager = userManager();

        UserLogin blockedUserLogin = blockedUser.login();
        UserLogin managerLogin = userManager.login();

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));

        when(userRepository.findUserByLogin(blockedUser.login()))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> userService.unBlockUserByLogin(managerLogin, blockedUserLogin));

        verify(userMapper, times(0)).toUserWithRolesView(blockedUser, List.of("CLIENT"));
        verify(userRepository, times(0)).blockUserById(blockedUser.id());
    }

    @Test
    void shouldNotUnBlockAlreadyUnBlockedUser() {
        UserEntity unblockedUser = mock(UserEntity.class);
        when(unblockedUser.isBlocked()).thenReturn(false);
        when(unblockedUser.id()).thenReturn((long) 35);
        when(unblockedUser.login()).thenReturn(new UserLogin("unBlockedUserLogin"));

        UserEntity userManager = userManager();

        UserLogin blockedUserLogin = unblockedUser.login();
        UserLogin managerLogin = userManager.login();

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));

        when(userRepository.findUserByLogin(unblockedUser.login()))
                .thenReturn(Optional.of(unblockedUser));

        when(userRepository.findAllUserRoles(unblockedUser)).thenReturn(List.of("CLIENT"));

        UserWithRolesView blockedUserWithRole = mock(UserWithRolesView.class);

        when(userMapper.toUserWithRolesView(unblockedUser, List.of("CLIENT"))).thenReturn(blockedUserWithRole);

        UserWithRolesView checkUserWithRolesView = userService.unBlockUserByLogin(managerLogin, blockedUserLogin);

        Assertions.assertEquals(blockedUserWithRole, checkUserWithRolesView);

        verify(userMapper, times(1)).toUserWithRolesView(unblockedUser, List.of("CLIENT"));
        verify(userRepository, times(0)).blockUserById(unblockedUser.id());
    }

    @Test
    void shouldRemoveManagerRole() {
        UserEntity user = user();
        UserEntity userManager = userManager();
        UserLogin userLogin = user.login();
        UserLogin managerLogin = userManager.login();
        RoleEntity managerRole = new RoleEntity(2L, "MANAGER");
        UserWithRolesView updatedUserWithRoles = new UserWithRolesView(
                user.id(),
                user.login(),
                user.name(),
                user.lastName(),
                List.of("CLIENT"),
                user.createdAt(),
                user.blockedAt()
        );

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));
        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.of(user))
                .thenReturn(Optional.of(user));
        when(userRepository.findAllUserRoles(user))
                .thenReturn(List.of("CLIENT", "MANAGER"))
                .thenReturn(List.of("CLIENT"));
        when(roleRepository.findRoleEntityByName("MANAGER"))
                .thenReturn(Optional.of(managerRole));
        when(userRepository.removeUserRole(user.id(), managerRole.id()))
                .thenReturn(true);
        when(userMapper.toUserWithRolesView(user, List.of("CLIENT")))
                .thenReturn(updatedUserWithRoles);

        UserWithRolesView result = userService.removeManagerRoleByLogin(managerLogin, userLogin);

        Assertions.assertEquals(updatedUserWithRoles, result);
        Assertions.assertEquals(user.id(), result.id());
        Assertions.assertEquals(user.login(), result.login());
        Assertions.assertEquals(user.name(), result.name());
        Assertions.assertEquals(user.lastName(), result.lastName());
        Assertions.assertEquals(user.createdAt(), result.createdAt());
        Assertions.assertEquals(user.blockedAt(), result.blockedAt());
        Assertions.assertEquals(List.of("CLIENT"), result.roles());

        verify(userRepository, times(1)).removeUserRole(user.id(), managerRole.id());
        verify(userMapper, times(1)).toUserWithRolesView(user, List.of("CLIENT"));
    }

    @Test
    void shouldNotRemoveManagerRoleWhenManagerNotFound() {
        UserEntity user = user();
        UserEntity userManager = userManager();
        UserLogin userLogin = user.login();
        UserLogin managerLogin = userManager.login();

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> userService.removeManagerRoleByLogin(managerLogin, userLogin)
        );

        Assertions.assertEquals("not found user with this login", exception.getMessage());

        verify(userRepository, times(0)).findUserByLogin(user.login());
        verify(userRepository, times(0)).removeUserRole(user.id(), 2L);
    }

    @Test
    void shouldNotRemoveManagerRoleWhenManagerIsBlocked() {
        UserEntity user = user();
        UserEntity blockedManager = new UserEntity(
                25L,
                SENIOR_MANAGER_LOGIN,
                "User",
                "Test",
                "password-hash",
                CREATED_AT,
                CREATED_AT
        );
        UserLogin userLogin = user.login();
        UserLogin managerLogin = blockedManager.login();

        when(userRepository.findUserByLogin(blockedManager.login()))
                .thenReturn(Optional.of(blockedManager));

        BusinessLogicException exception = Assertions.assertThrows(
                BusinessLogicException.class,
                () -> userService.removeManagerRoleByLogin(managerLogin, userLogin)
        );

        Assertions.assertEquals("user senior_manager is blocked", exception.getMessage());

        verify(userRepository, times(0)).findUserByLogin(user.login());
        verify(userRepository, times(0)).removeUserRole(user.id(), 2L);
    }

    @Test
    void shouldNotRemoveManagerRoleWhenUserHasNotManagerRole() {
        UserEntity user = user();
        UserEntity userManager = userManager();
        UserLogin userLogin = user.login();
        UserLogin managerLogin = userManager.login();

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));
        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.of(user));
        when(userRepository.findAllUserRoles(user)).thenReturn(List.of("CLIENT"));

        BusinessLogicException exception = Assertions.assertThrows(
                BusinessLogicException.class,
                () -> userService.removeManagerRoleByLogin(managerLogin, userLogin)
        );

        Assertions.assertEquals("user has not manager role", exception.getMessage());

        verify(roleRepository, times(0)).findRoleEntityByName("MANAGER");
        verify(userRepository, times(0)).removeUserRole(user.id(), 2L);
    }

    @Test
    void shouldNotRemoveManagerRoleWhenUserNotFound() {
        UserEntity user = user();
        UserEntity userManager = userManager();
        UserLogin userLogin = user.login();
        UserLogin managerLogin = userManager.login();

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));
        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> userService.removeManagerRoleByLogin(managerLogin, userLogin)
        );

        Assertions.assertEquals("not found user with this login", exception.getMessage());

        verify(userRepository, times(0)).removeUserRole(user.id(), 2L);
    }

    @Test
    void shouldThrowExceptionWhenManagerRoleNotFound() {
        UserEntity user = user();
        UserEntity userManager = userManager();
        UserLogin userLogin = user.login();
        UserLogin managerLogin = userManager.login();

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));
        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.of(user));
        when(userRepository.findAllUserRoles(user)).thenReturn(List.of("CLIENT", "MANAGER"));
        when(roleRepository.findRoleEntityByName("MANAGER"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> userService.removeManagerRoleByLogin(managerLogin, userLogin)
        );

        Assertions.assertEquals("role with name MANAGER not found", exception.getMessage());

        verify(userRepository, times(0)).removeUserRole(user.id(), 2L);
    }

    @Test
    void shouldThrowExceptionWhenManagerRoleNotRemoved() {
        UserEntity user = user();
        UserEntity userManager = userManager();
        UserLogin userLogin = user.login();
        UserLogin managerLogin = userManager.login();
        RoleEntity managerRole = new RoleEntity(2L, "MANAGER");

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));
        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.of(user));
        when(userRepository.findAllUserRoles(user)).thenReturn(List.of("CLIENT", "MANAGER"));
        when(roleRepository.findRoleEntityByName("MANAGER"))
                .thenReturn(Optional.of(managerRole));
        when(userRepository.removeUserRole(user.id(), managerRole.id()))
                .thenReturn(false);

        BusinessLogicException exception = Assertions.assertThrows(
                BusinessLogicException.class,
                () -> userService.removeManagerRoleByLogin(managerLogin, userLogin)
        );

        Assertions.assertEquals("user manager role not removed", exception.getMessage());

        verify(userRepository, times(1)).removeUserRole(user.id(), managerRole.id());
    }
}
