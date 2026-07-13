package ru.formatkoda.polybank.service.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.polybank.domain.user.RoleEntity;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.messaging.publisher.UserEventPublisher;
import ru.formatkoda.polybank.repository.RoleRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.service.UserService;
import ru.formatkoda.polybank.util.mapper.UserMapper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    UserEventPublisher userEventPublisher;

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

        UserWithRolesView result = userService.removeUserRole(managerLogin, userLogin, "MANAGER");

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
    void shouldRemoveClientRole() {
        UserEntity user = user();
        UserEntity userManager = userManager();
        UserLogin userLogin = user.login();
        UserLogin managerLogin = userManager.login();
        RoleEntity clientRole = new RoleEntity(1L, "CLIENT");
        UserWithRolesView updatedUserWithRoles = new UserWithRolesView(
                user.id(),
                user.login(),
                user.name(),
                user.lastName(),
                List.of("MANAGER"),
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
                .thenReturn(List.of("MANAGER"));
        when(roleRepository.findRoleEntityByName("CLIENT"))
                .thenReturn(Optional.of(clientRole));
        when(userRepository.removeUserRole(user.id(), clientRole.id()))
                .thenReturn(true);
        when(userMapper.toUserWithRolesView(user, List.of("MANAGER")))
                .thenReturn(updatedUserWithRoles);

        UserWithRolesView result = userService.removeUserRole(managerLogin, userLogin, "CLIENT");

        Assertions.assertEquals(updatedUserWithRoles, result);
        Assertions.assertEquals(List.of("MANAGER"), result.roles());

        verify(userRepository, times(1)).removeUserRole(user.id(), clientRole.id());
        verify(userMapper, times(1)).toUserWithRolesView(user, List.of("MANAGER"));
    }

    @Test
    void shouldNotRemoveRoleFromHimself() {
        UserEntity user = user();
        UserLogin userLogin = user.login();

        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.of(user));

        BusinessLogicException exception = Assertions.assertThrows(
                BusinessLogicException.class,
                () -> userService.removeUserRole(userLogin, userLogin, "MANAGER")
        );

        Assertions.assertEquals("user can not remove himself", exception.getMessage());

        verify(userRepository, times(0)).findAllUserRoles(user);
        verify(roleRepository, times(0)).findRoleEntityByName("MANAGER");
        verify(userRepository, times(0)).removeUserRole(user.id(), 2L);
    }

    @Test
    void addUserRoleShouldBindRoleAndReturnUpdatedUser() {
        UserEntity user = user();
        UserEntity manager = userManager();

        UserLogin userLogin = user.login();
        UserLogin managerLogin = manager.login();

        String roleName = "SLAVE_MANAGER";
        long roleId = 10L;

        RoleEntity role = new RoleEntity(roleId, roleName);

        List<String> currentRoles = List.of("CLIENT", "MANAGER");
        List<String> updatedRoles = List.of(
                "CLIENT",
                "MANAGER",
                "SLAVE_MANAGER"
        );

        UserWithRolesView expectedResult = new UserWithRolesView(
                user.id(),
                user.login(),
                user.name(),
                user.lastName(),
                updatedRoles,
                user.createdAt(),
                user.blockedAt()
        );

        when(userRepository.findUserByLogin(managerLogin))
                .thenReturn(Optional.of(manager));

        when(userRepository.findUserByLogin(userLogin))
                .thenReturn(Optional.of(user))
                .thenReturn(Optional.of(user));

        when(userRepository.findAllUserRoles(user))
                .thenReturn(currentRoles)
                .thenReturn(updatedRoles);

        when(roleRepository.findRoleEntityByName(roleName))
                .thenReturn(Optional.of(role));

        when(userMapper.toUserWithRolesView(user, updatedRoles))
                .thenReturn(expectedResult);

        UserWithRolesView result = userService.addUserRole(
                managerLogin,
                userLogin,
                roleName
        );

        assertThat(result).isSameAs(expectedResult);

        verify(userRepository).bindUserWithRole(user.id(), roleId);
        verify(userMapper).toUserWithRolesView(user, updatedRoles);
    }

    @Test
    void addUserRoleShouldThrowExceptionWhenManagerAddsRoleToHimself() {
        UserEntity manager = userManager();

        UserLogin managerLogin = manager.login();
        String roleName = "SLAVE_MANAGER";

        when(userRepository.findUserByLogin(managerLogin))
                .thenReturn(Optional.of(manager))
                .thenReturn(Optional.of(manager));

        assertThatThrownBy(() ->
                userService.addUserRole(
                        managerLogin,
                        managerLogin,
                        roleName
                )
        )
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("user can not add role himself");

        verify(userRepository, never())
                .bindUserWithRole(anyLong(), anyLong());

        verifyNoInteractions(roleRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void addUserRoleShouldThrowExceptionWhenUserAlreadyHasRole() {
        UserEntity user = user();
        UserEntity manager = userManager();

        UserLogin userLogin = user.login();
        UserLogin managerLogin = manager.login();

        String roleName = "MANAGER";

        when(userRepository.findUserByLogin(managerLogin))
                .thenReturn(Optional.of(manager));

        when(userRepository.findUserByLogin(userLogin))
                .thenReturn(Optional.of(user));

        when(userRepository.findAllUserRoles(user))
                .thenReturn(List.of("CLIENT", "MANAGER"));

        assertThatThrownBy(() ->
                userService.addUserRole(
                        managerLogin,
                        userLogin,
                        roleName
                )
        ).isInstanceOf(BusinessLogicException.class)
                .hasMessage("user already has MANAGER role");

        verify(userRepository, never())
                .bindUserWithRole(anyLong(), anyLong());

        verifyNoInteractions(roleRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void addUserRoleShouldThrowExceptionWhenRoleNotFound() {
        UserEntity user = user();
        UserEntity manager = userManager();

        UserLogin userLogin = user.login();
        UserLogin managerLogin = manager.login();

        String roleName = "UNKNOWN_ROLE";

        when(userRepository.findUserByLogin(managerLogin))
                .thenReturn(Optional.of(manager));

        when(userRepository.findUserByLogin(userLogin))
                .thenReturn(Optional.of(user));

        when(userRepository.findAllUserRoles(user))
                .thenReturn(List.of("CLIENT"));

        when(roleRepository.findRoleEntityByName(roleName))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.addUserRole(
                        managerLogin,
                        userLogin,
                        roleName
                )
        ).isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never())
                .bindUserWithRole(anyLong(), anyLong());

        verifyNoInteractions(userMapper);
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
                () -> userService.removeUserRole(managerLogin, userLogin, "MANAGER")
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
                () -> userService.removeUserRole(managerLogin, userLogin, "MANAGER")
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
                () -> userService.removeUserRole(managerLogin, userLogin, "MANAGER")
        );

        Assertions.assertEquals("user has not MANAGER role", exception.getMessage());

        verify(roleRepository, times(0)).findRoleEntityByName("MANAGER");
        verify(userRepository, times(0)).removeUserRole(user.id(), 2L);
    }

    @Test
    void shouldNotRemoveManagerRoleWhenUserHasOnlyManagerRole() {
        UserEntity user = user();
        UserEntity userManager = userManager();
        UserLogin userLogin = user.login();
        UserLogin managerLogin = userManager.login();

        when(userRepository.findUserByLogin(userManager.login()))
                .thenReturn(Optional.of(userManager));
        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.of(user));
        when(userRepository.findAllUserRoles(user)).thenReturn(List.of("MANAGER"));

        BusinessLogicException exception = Assertions.assertThrows(
                BusinessLogicException.class,
                () -> userService.removeUserRole(managerLogin, userLogin, "MANAGER")
        );

        Assertions.assertEquals("user has only one role, use block instead", exception.getMessage());

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
                () -> userService.removeUserRole(managerLogin, userLogin, "MANAGER")
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
                () -> userService.removeUserRole(managerLogin, userLogin, "MANAGER")
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
                () -> userService.removeUserRole(managerLogin, userLogin, "MANAGER")
        );

        Assertions.assertEquals("user MANAGER role not removed", exception.getMessage());

        verify(userRepository, times(1)).removeUserRole(user.id(), managerRole.id());
    }

    @Test
    void shouldCreateManagerBySeniorManager() {
        UserEntity seniorManager = userManager();
        UserEntity managerToCreate = managerToCreate();
        UserEntity createdManager = createdManager();
        RoleEntity managerRole = new RoleEntity(2L, "MANAGER");
        UserWithRolesView createdManagerWithRoles = new UserWithRolesView(
                createdManager.id(),
                createdManager.login(),
                createdManager.name(),
                createdManager.lastName(),
                List.of("MANAGER"),
                createdManager.createdAt(),
                createdManager.blockedAt()
        );

        when(userRepository.findUserByLogin(seniorManager.login()))
                .thenReturn(Optional.of(seniorManager));
        when(userRepository.findUserByLogin(managerToCreate.login()))
                .thenReturn(Optional.empty());
        when(roleRepository.findRoleEntityByName("MANAGER"))
                .thenReturn(Optional.of(managerRole));
        when(userRepository.createUser(managerToCreate))
                .thenReturn(Optional.of(createdManager));
        when(userRepository.findAllUserRoles(createdManager))
                .thenReturn(List.of("MANAGER"));
        when(userMapper.toUserWithRolesView(createdManager, List.of("MANAGER")))
                .thenReturn(createdManagerWithRoles);

        UserWithRolesView result = userService.createStaffUser(
                seniorManager.login(),
                managerToCreate,
                "MANAGER"
        );

        Assertions.assertEquals(createdManagerWithRoles, result);
        Assertions.assertEquals(List.of("MANAGER"), result.roles());
        Assertions.assertNull(result.blockedAt());

        verify(roleRepository, times(1)).findRoleEntityByName("MANAGER");
        verify(userRepository, times(1)).createUser(managerToCreate);
        verify(userRepository, times(1)).bindUserWithRole(createdManager.id(), managerRole.id());
        verify(userMapper, times(1)).toUserWithRolesView(createdManager, List.of("MANAGER"));
        verify(userEventPublisher).publishUserRegistered(createdManager, "MANAGER");
    }

    @Test
    void createStaffUserShouldBeTransactional() throws NoSuchMethodException {
        Method method = UserService.class.getMethod(
                "createStaffUser",
                UserLogin.class,
                UserEntity.class,
                String.class
        );
        Transactional transactional = method.getAnnotation(Transactional.class);

        Assertions.assertNotNull(transactional);
        Assertions.assertFalse(transactional.readOnly());
    }

    @Test
    void shouldNotCreateManagerWhenSeniorManagerIsBlocked() {
        UserEntity blockedSeniorManager = new UserEntity(
                25L,
                SENIOR_MANAGER_LOGIN,
                "User",
                "Test",
                "password-hash",
                CREATED_AT,
                CREATED_AT
        );
        UserEntity managerToCreate = managerToCreate();
        UserLogin managerLogin = blockedSeniorManager.login();

        when(userRepository.findUserByLogin(managerLogin))
                .thenReturn(Optional.of(blockedSeniorManager));

        BusinessLogicException exception = Assertions.assertThrows(
                BusinessLogicException.class,
                () -> userService.createStaffUser(managerLogin, managerToCreate, "MANAGER")
        );

        Assertions.assertEquals("user senior_manager is blocked", exception.getMessage());

        verify(userRepository, never()).findUserByLogin(managerToCreate.login());
        verify(roleRepository, never()).findRoleEntityByName("MANAGER");
        verify(userRepository, never()).createUser(any());
        verify(userRepository, never()).bindUserWithRole(anyLong(), anyLong());
    }

    @Test
    void shouldNotCreateManagerWhenLoginAlreadyExists() {
        UserEntity seniorManager = userManager();
        UserEntity managerToCreate = managerToCreate();
        UserLogin managerLogin = seniorManager.login();

        when(userRepository.findUserByLogin(managerLogin))
                .thenReturn(Optional.of(seniorManager));
        when(userRepository.findUserByLogin(managerToCreate.login()))
                .thenReturn(Optional.of(managerToCreate));

        BusinessLogicException exception = Assertions.assertThrows(
                BusinessLogicException.class,
                () -> userService.createStaffUser(managerLogin, managerToCreate, "MANAGER")
        );

        Assertions.assertEquals("user with given login already exists", exception.getMessage());

        verify(roleRepository, never()).findRoleEntityByName("MANAGER");
        verify(userRepository, never()).createUser(any());
        verify(userRepository, never()).bindUserWithRole(anyLong(), anyLong());
    }

    @Test
    void shouldNotCreateManagerWhenManagerRoleNotFound() {
        UserEntity seniorManager = userManager();
        UserEntity managerToCreate = managerToCreate();
        UserLogin managerLogin = seniorManager.login();

        when(userRepository.findUserByLogin(managerLogin))
                .thenReturn(Optional.of(seniorManager));
        when(userRepository.findUserByLogin(managerToCreate.login()))
                .thenReturn(Optional.empty());
        when(roleRepository.findRoleEntityByName("MANAGER"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> userService.createStaffUser(managerLogin, managerToCreate, "MANAGER")
        );

        Assertions.assertEquals("role with name MANAGER not found", exception.getMessage());

        verify(userRepository, never()).createUser(any());
        verify(userRepository, never()).bindUserWithRole(anyLong(), anyLong());
    }

    @Test
    void shouldNotCreateClientRoleViaManagerCreationEndpoint() {
        UserEntity seniorManager = userManager();
        UserEntity managerToCreate = managerToCreate();
        UserLogin managerLogin = seniorManager.login();

        when(userRepository.findUserByLogin(managerLogin))
                .thenReturn(Optional.of(seniorManager));

        BusinessLogicException exception = Assertions.assertThrows(
                BusinessLogicException.class,
                () -> userService.createStaffUser(managerLogin, managerToCreate, "CLIENT")
        );

        Assertions.assertEquals("only staff user can be created by manager", exception.getMessage());

        verify(roleRepository, never()).findRoleEntityByName("CLIENT");
        verify(userRepository, never()).createUser(any());
        verify(userRepository, never()).bindUserWithRole(anyLong(), anyLong());
    }

    @Test
    void shouldNotBindRoleWhenManagerUserWasNotCreated() {
        UserEntity seniorManager = userManager();
        UserEntity managerToCreate = managerToCreate();
        UserLogin managerLogin = seniorManager.login();
        RoleEntity managerRole = new RoleEntity(2L, "MANAGER");

        when(userRepository.findUserByLogin(managerLogin))
                .thenReturn(Optional.of(seniorManager));
        when(userRepository.findUserByLogin(managerToCreate.login()))
                .thenReturn(Optional.empty());
        when(roleRepository.findRoleEntityByName("MANAGER"))
                .thenReturn(Optional.of(managerRole));
        when(userRepository.createUser(managerToCreate))
                .thenReturn(Optional.empty());

        BusinessLogicException exception = Assertions.assertThrows(
                BusinessLogicException.class,
                () -> userService.createStaffUser(managerLogin, managerToCreate, "MANAGER")
        );

        Assertions.assertEquals("user not created", exception.getMessage());

        verify(userRepository, times(1)).createUser(managerToCreate);
        verify(userRepository, never()).bindUserWithRole(anyLong(), anyLong());
        verify(userMapper, never()).toUserWithRolesView(any(), any());
    }

    @Test
    void shouldNotCompleteWhenManagerRoleBindingFails() {
        UserEntity seniorManager = userManager();
        UserEntity managerToCreate = managerToCreate();
        UserEntity createdManager = createdManager();
        UserLogin managerLogin = seniorManager.login();
        RoleEntity managerRole = new RoleEntity(2L, "MANAGER");

        when(userRepository.findUserByLogin(managerLogin))
                .thenReturn(Optional.of(seniorManager));
        when(userRepository.findUserByLogin(managerToCreate.login()))
                .thenReturn(Optional.empty());
        when(roleRepository.findRoleEntityByName("MANAGER"))
                .thenReturn(Optional.of(managerRole));
        when(userRepository.createUser(managerToCreate))
                .thenReturn(Optional.of(createdManager));
        doThrow(new BusinessLogicException("user role not created"))
                .when(userRepository)
                .bindUserWithRole(createdManager.id(), managerRole.id());

        BusinessLogicException exception = Assertions.assertThrows(
                BusinessLogicException.class,
                () -> userService.createStaffUser(managerLogin, managerToCreate, "MANAGER")
        );

        Assertions.assertEquals("user role not created", exception.getMessage());

        verify(userRepository, times(1)).createUser(managerToCreate);
        verify(userRepository, times(1)).bindUserWithRole(createdManager.id(), managerRole.id());
        verify(userMapper, never()).toUserWithRolesView(any(), any());
    }

    private UserEntity managerToCreate() {
        return new UserEntity(
                null,
                new UserLogin("new_manager"),
                "Manager",
                "Test",
                "password-hash",
                CREATED_AT,
                null
        );
    }

    private UserEntity createdManager() {
        return new UserEntity(
                30L,
                new UserLogin("new_manager"),
                "Manager",
                "Test",
                "password-hash",
                CREATED_AT,
                null
        );
    }
}
