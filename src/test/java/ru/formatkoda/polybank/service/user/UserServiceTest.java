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
import ru.formatkoda.polybank.repository.RoleRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.service.UserService;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.testutil.TestData;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.util.pagination.PageResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static ru.formatkoda.polybank.testutil.TestData.user;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    RoleRepository roleRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Test
    void findAllUsersWithRolesShouldReturnPageResultWithMappedUsers() {
        PageRequest pageRequest = new PageRequest(0, 10);

        UserEntity user1 = TestData.user();

        UserEntity user2 = TestData.user();

        when(userRepository.findAllUsers(pageRequest))
                .thenReturn(List.of(user1, user2));

        PageResult<UserWithRolesView> result =
                userService.findAllUsersWithRoles(pageRequest);

        assertThat(result).isNotNull();

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.total()).isEqualTo(2);

        assertThat(result.items().get(0).login()).isEqualTo(TestData.USER_LOGIN);

        assertThat(result.items().get(1).login()).isEqualTo(TestData.USER_LOGIN);

        verify(userRepository).findAllUsers(pageRequest);
    }

    @Test
    void findAllUsersWithRolesShouldReturnEmptyPageResultWhenRepositoryReturnsEmptyList() {
        PageRequest pageRequest = new PageRequest(1, 5);

        when(userRepository.findAllUsers(pageRequest))
                .thenReturn(List.of());

        PageResult<UserWithRolesView> result =
                userService.findAllUsersWithRoles(pageRequest);

        assertThat(result).isNotNull();

        Assertions.assertTrue(result.items().isEmpty());
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.total()).isZero();

        verify(userRepository).findAllUsers(pageRequest);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void createUserShouldThrowExceptionWhenLoginIsDuplicated() {
        UserEntity user = user();

        when(userRepository.findUserByLogin(user.login()))
                .thenReturn(Optional.of(user));

        BusinessLogicException actualException = Assertions.assertThrows(BusinessLogicException.class,
                () -> userService.createUser(user));

        Assertions.assertEquals("user already exists", actualException.getMessage());
    }

    @Test
    void createUserShouldThrowExceptionWhenRoleNotFound() {
        UserEntity user = user();

        when(roleRepository.findRoleEntityByName("CLIENT"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException actualException = Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> userService.createUser(user));
        Assertions.assertEquals("not found CLIENT role", actualException.getMessage());
    }

    @Test
    void createUserShouldCallingRepositoriesMethods() {
        prepareMocksForCallingCreateUser(user().login());

        userService.createUser(user());

        verify(userRepository).findUserByLogin(user().login());
        verify(roleRepository).findRoleEntityByName("CLIENT");
        verify(userRepository).createUserAndReturnId(user());
        verify(userRepository).bindUserWithRole(anyLong(), anyLong());
    }

    @Test
    void createUserShouldReturnUserLoginMatchesWithUserEntityLogin() {
        prepareMocksForCallingCreateUser(user().login());

        UserLogin userLogin = userService.createUser(user());

        Assertions.assertNotNull(userLogin);
        Assertions.assertEquals(userLogin, user().login());
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

        ResourceNotFoundException actualException = Assertions
                .assertThrows(ResourceNotFoundException.class,
                        () -> userService.findUserByLogin(login));

        Assertions.assertEquals("not found user with this login", actualException.getMessage());
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
                .thenReturn(1L);
        doNothing().when(userRepository).bindUserWithRole(anyLong(), anyLong());
    }
}
