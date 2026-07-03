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

import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.repository.RoleRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.service.UserService;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
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

        BusinessLogicException actualException = Assertions.assertThrows(BusinessLogicException.class,
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

        BusinessLogicException actualException = Assertions
                .assertThrows(BusinessLogicException.class,
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
