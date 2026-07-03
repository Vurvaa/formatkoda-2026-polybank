package ru.formatkoda.polybank.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.service.UserService;

@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {
    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userService.findUserByLogin(username);

        return new AuthUserDetails(
                userEntity.login(),
                userEntity.passwordHash(),
                userEntity.isBlocked()
        );
    }
}
