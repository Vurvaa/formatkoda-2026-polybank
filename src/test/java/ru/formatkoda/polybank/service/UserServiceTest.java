package ru.formatkoda.polybank.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.polybank.domain.user.RoleEntity;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;

import ru.formatkoda.polybank.exceptions.BusinessLogicException;
import ru.formatkoda.polybank.repository.RoleRepository;
import ru.formatkoda.polybank.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    RoleRepository roleRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    private final UserEntity userEntity = new UserEntity(
            null,
            "login",
            "name",
            "lastName",
            "password",
            null,
            null
    );

    @Test
    void createUserShouldThrowExceptionWhenLoginIsDuplicated() {
        String expectedLogin = userEntity.login();
        String expectedExceptionMassage = "login already exists";

        when(userRepository.findUserByLogin(expectedLogin))
                .thenReturn(Optional.of(userEntity));

        BusinessLogicException actualException = Assertions.assertThrows(BusinessLogicException.class,
                () -> userService.createUser(userEntity));

        Assertions.assertEquals(expectedExceptionMassage, actualException.getMessage());
    }

    @Test
    void createUserShouldThrowExceptionWhenRoleNotFound() {
        String expectedExceptionMassage = "not found CLIENT role";

        when(roleRepository.findRoleEntityByName("CLIENT"))
                .thenReturn(Optional.empty());

        BusinessLogicException actualException = Assertions.assertThrows(BusinessLogicException.class,
                () -> userService.createUser(userEntity));
        Assertions.assertEquals(expectedExceptionMassage, actualException.getMessage());
    }

    @Test
    void createUserShouldCallingRepositoriesMethods() {
        String expectedLogin = userEntity.login();
        prepareMocksForCallingCreateUser(expectedLogin);

        userService.createUser(userEntity);

        verify(userRepository).findUserByLogin(expectedLogin);
        verify(roleRepository).findRoleEntityByName("CLIENT");
        verify(userRepository).createUserAndReturnId(userEntity);
        verify(userRepository).bindUserWithRole(anyLong(), anyLong());
    }

    @Test
    void createUserShouldReturnUserLoginMatchesWithUserEntityLogin() {
        String expectedLogin = userEntity.login();
        prepareMocksForCallingCreateUser(expectedLogin);

        UserLogin userLogin = userService.createUser(userEntity);
        String actualLogin = userLogin.login();

        Assertions.assertNotNull(userLogin);
        Assertions.assertEquals(expectedLogin, actualLogin);
    }

    @Test
    void findUserByLoginShouldCallingUserRepositoryMethod() {
        String expectedLogin = userEntity.login();
        when(userRepository.findUserByLogin(expectedLogin))
                .thenReturn(Optional.of(userEntity));

        userService.findUserByLogin(expectedLogin);

        verify(userRepository).findUserByLogin(expectedLogin);
    }

    @Test
    void findUserByLoginShouldThrowingExceptionWhenUserNotFound() {
        String expectedLogin = userEntity.login();
        String expectedExceptionMassage = "not found user with this login";

        when(userRepository.findUserByLogin(expectedLogin))
                .thenReturn(Optional.empty());

        BusinessLogicException actualException = Assertions
                .assertThrows(BusinessLogicException.class,
                        () -> userService.findUserByLogin(expectedLogin));

        Assertions.assertEquals(expectedExceptionMassage, actualException.getMessage());
    }

    @Test
    void findUserByLoginShouldReturnUserWhenUserExists() {
        String expectedLogin = userEntity.login();

        when(userRepository.findUserByLogin(expectedLogin))
                .thenReturn(Optional.of(userEntity));

        UserEntity result = userService.findUserByLogin(expectedLogin);

        assertThat(result).isSameAs(userEntity);

        verify(userRepository).findUserByLogin(expectedLogin);
    }

    private void prepareMocksForCallingCreateUser(String expectedLogin) {
        when(userRepository.findUserByLogin(expectedLogin))
                .thenReturn(Optional.empty());

        RoleEntity expectedRole = new RoleEntity(
                1L,
                "CLIENT"
        );

        when(roleRepository.findRoleEntityByName("CLIENT"))
                .thenReturn(Optional.of(expectedRole));
        when(userRepository.createUserAndReturnId(userEntity))
                .thenReturn(1L);
        doNothing().when(userRepository).bindUserWithRole(anyLong(), anyLong());
    }
}
