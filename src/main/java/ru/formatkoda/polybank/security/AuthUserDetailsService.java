package ru.formatkoda.polybank.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.service.UserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {
    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserEntity userEntity = userService.findUserByLogin(new UserLogin(username));
        List<String> userRoles = userService.findAllUserRoles(userEntity);

        return new AuthUserDetails(
                userEntity.login().value(),
                userEntity.passwordHash(),
                userEntity.isBlocked(),
                userRoles.stream().map(s -> new SimpleGrantedAuthority("ROLE_" + s)).toList()
        );
    }
}
