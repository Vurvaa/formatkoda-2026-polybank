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
            new UserLogin("value"),
            "name",
            "lastName",
            "password",
            null,
            null
    );

    @Test
    void createUserShouldThrowExceptionWhenLoginIsDuplicated() {
        when(userRepository.findUserByLogin(userEntity.login()))
                .thenReturn(Optional.of(userEntity));

        BusinessLogicException actualException = Assertions.assertThrows(BusinessLogicException.class,
                () -> userService.createUser(userEntity));

        Assertions.assertEquals("user already exists", actualException.getMessage());
    }

    @Test
    void createUserShouldThrowExceptionWhenRoleNotFound() {
        when(roleRepository.findRoleEntityByName("CLIENT"))
                .thenReturn(Optional.empty());

        BusinessLogicException actualException = Assertions.assertThrows(BusinessLogicException.class,
                () -> userService.createUser(userEntity));
        Assertions.assertEquals("not found CLIENT role", actualException.getMessage());
    }

    @Test
    void createUserShouldCallingRepositoriesMethods() {
        prepareMocksForCallingCreateUser(userEntity.login());

        userService.createUser(userEntity);

        verify(userRepository).findUserByLogin(userEntity.login());
        verify(roleRepository).findRoleEntityByName("CLIENT");
        verify(userRepository).createUserAndReturnId(userEntity);
        verify(userRepository).bindUserWithRole(anyLong(), anyLong());
    }

    @Test
    void createUserShouldReturnUserLoginMatchesWithUserEntityLogin() {
        prepareMocksForCallingCreateUser(userEntity.login());

        UserLogin userLogin = userService.createUser(userEntity);

        Assertions.assertNotNull(userLogin);
        Assertions.assertEquals(userLogin, userEntity.login());
    }

    @Test
    void findUserByLoginShouldCallingUserRepositoryMethod() {
        when(userRepository.findUserByLogin(userEntity.login()))
                .thenReturn(Optional.of(userEntity));

        userService.findUserByLogin(userEntity.login());

        verify(userRepository).findUserByLogin(userEntity.login());
    }

    @Test
    void findUserByLoginShouldThrowingExceptionWhenUserNotFound() {
        when(userRepository.findUserByLogin(userEntity.login()))
                .thenReturn(Optional.empty());

        UserLogin login = userEntity.login();

        BusinessLogicException actualException = Assertions
                .assertThrows(BusinessLogicException.class,
                        () -> userService.findUserByLogin(login));

        Assertions.assertEquals("not found user with this value", actualException.getMessage());
    }

    @Test
    void findUserByLoginShouldReturnUserWhenUserExists() {
        when(userRepository.findUserByLogin(userEntity.login()))
                .thenReturn(Optional.of(userEntity));

        UserEntity result = userService.findUserByLogin(userEntity.login());

        assertThat(result).isSameAs(userEntity);

        verify(userRepository).findUserByLogin(userEntity.login());
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
        when(userRepository.createUserAndReturnId(userEntity))
                .thenReturn(1L);
        doNothing().when(userRepository).bindUserWithRole(anyLong(), anyLong());
    }
}
