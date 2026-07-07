package ru.formatkoda.polybank.service.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.service.UserService;
import ru.formatkoda.polybank.util.mapper.UserMapper;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static ru.formatkoda.polybank.testutil.TestData.user;
import static ru.formatkoda.polybank.testutil.TestData.userManager;

@ExtendWith(MockitoExtension.class)
class UserServiceManagerOperationsTest {
    @Mock
    UserRepository userRepository;

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
}
