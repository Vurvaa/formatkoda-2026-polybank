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
import ru.formatkoda.polybank.messaging.publisher.UserEventPublisher;
import ru.formatkoda.polybank.repository.RoleRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.service.UserService;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.testutil.TestData;
import ru.formatkoda.polybank.util.mapper.UserMapper;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static ru.formatkoda.polybank.testutil.TestData.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    RoleRepository roleRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    UserEventPublisher userEventPublisher;

    @InjectMocks
    UserService userService;

    @Test
    void findAllUsersWithRolesShouldReturnPageResultWithMappedUsers() {
        PageRequest pageRequest = new PageRequest(0, 10);

        UserEntity user1 = TestData.user();
        UserEntity user2 = TestData.user();

        when(userRepository.findAllUsers(pageRequest))
                .thenReturn(List.of(user1, user2));
        when(userRepository.countAll()).thenReturn(2L);

        when(userMapper.toUserWithRolesView(any(), any()))
                .thenReturn(TestData.userWithRoles());

        PageResult<UserWithRolesView> result =
                userService.findAllUsersWithRoles(pageRequest);

        assertThat(result).isNotNull();

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.total()).isEqualTo(2);

        verify(userRepository).findAllUsers(pageRequest);
        verify(userRepository).countAll();
    }

    @Test
    void findAllUsersWithRolesShouldReturnEmptyPageResultWhenRepositoryReturnsEmptyList() {
        PageRequest pageRequest = new PageRequest(1, 5);

        when(userRepository.findAllUsers(pageRequest))
                .thenReturn(List.of());
        when(userRepository.countAll()).thenReturn(0L);

        PageResult<UserWithRolesView> result =
                userService.findAllUsersWithRoles(pageRequest);

        assertThat(result).isNotNull();

        Assertions.assertTrue(result.items().isEmpty());
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.total()).isZero();

        verify(userRepository).findAllUsers(pageRequest);
        verify(userRepository).countAll();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void findUserWithRolesSuccessReturnsView() {
        UserEntity user = user();
        UserWithRolesView expectedUser = userWithRoles();
        List<String> roles = List.of("CLIENT");

        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.of(user));

        when(userRepository.findAllUserRoles(user))
                .thenReturn(roles);

        when(userMapper.toUserWithRolesView(user, roles))
                .thenReturn(expectedUser);

        UserWithRolesView result = userService.findUserWithRoles(user.login());

        assertNotNull(result);
        verify(userMapper).toUserWithRolesView(user, roles);
    }

    @Test
    void findUserWithRolesUserNotFoundThrowsException() {
        UserEntity user = user();
        UserLogin login = user.login();
        String expectedMessage = "not found user with this login";

        when(userRepository.findUserByLogin(user().login()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.findUserWithRoles(login));

        assertEquals(expectedMessage, exception.getMessage());
        verify(userMapper, never()).toUserWithRolesView(any(), any());
    }

        @Test
    void createUserShouldThrowExceptionWhenLoginIsDuplicated() {
        UserEntity user = user();

        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.of(user));

        BusinessLogicException actualException = assertThrows(BusinessLogicException.class,
                () -> userService.createUser(user));

        assertEquals("user already exists", actualException.getMessage());
    }

    @Test
    void createUserShouldThrowExceptionWhenRoleNotFound() {
        UserEntity user = user();

        when(roleRepository.findRoleEntityByName("CLIENT"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException actualException = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.createUser(user));
        assertEquals("role with name CLIENT not found", actualException.getMessage());
    }

    @Test
    void createUserShouldCallingRepositoriesMethods() {
        prepareMocksForCallingCreateUser(user().login());

        userService.createUser(user());

        verify(userRepository).findUserByLogin(user().login());
        verify(roleRepository).findRoleEntityByName("CLIENT");
        verify(userRepository).createUserAndReturnId(user());
        verify(userRepository).bindUserWithRole(anyLong(), anyLong());
        verify(userEventPublisher).publishUserRegistered(user(), "CLIENT");
    }

    @Test
    void createUserShouldReturnUserLoginMatchesWithUserEntityLogin() {
        prepareMocksForCallingCreateUser(user().login());

        UserLogin userLogin = userService.createUser(user());

        verify(userEventPublisher).publishUserRegistered(user(), "CLIENT");

        assertNotNull(userLogin);
        assertEquals(userLogin, user().login());
    }

    @Test
    void findUserByLoginShouldCallingUserRepositoryMethod() {
        when(userRepository.findUserByLogin(user().login()))
                .thenReturn(Optional.of(user()));

        userService.findUserByLogin(user().login());

        verify(userRepository).findUserByLogin(user().login());
    }

    @Test
    void findUserByLoginShouldThrowingExceptionWhenUserNotFound() {
        when(userRepository.findUserByLogin(user().login()))
                .thenReturn(Optional.empty());

        UserLogin login = user().login();

        ResourceNotFoundException actualException = assertThrows(ResourceNotFoundException.class,
                        () -> userService.findUserByLogin(login));

        assertEquals("not found user with this login", actualException.getMessage());
    }

    @Test
    void findUserByLoginShouldReturnUserWhenUserExists() {
        UserEntity user = user();

        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.of(user));

        UserEntity result = userService.findUserByLogin(user.login());

        assertThat(result).isSameAs(user);

        verify(userRepository).findUserByLogin(user.login());
    }

    private void prepareMocksForCallingCreateUser(UserLogin expectedLogin) {
        when(userRepository.findUserByLogin(expectedLogin))
                .thenReturn(Optional.empty());

        RoleEntity expectedRole = new RoleEntity(
                1L,
                "CLIENT"
        );

        when(roleRepository.findRoleEntityByName("CLIENT"))
                .thenReturn(Optional.of(expectedRole));
        when(userRepository.createUserAndReturnId(user()))
                .thenReturn(Optional.of(1L));
        doNothing().when(userRepository).bindUserWithRole(anyLong(), anyLong());
    }
}
