package ru.formatkoda.authorization.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.formatkoda.authorization.auth.repository.UserRepository;
import ru.formatkoda.authorization.auth.domain.UserEntity;
import ru.formatkoda.authorization.auth.domain.UserLogin;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserEntity userEntity = userRepository.findUserByLogin(new UserLogin(username))
                .orElseThrow(() -> new UsernameNotFoundException("not found user with this login"));

        List<String> userRoles = userRepository.findAllUserRoles(userEntity);

        return new CustomUserDetails(
                userEntity.login().value(),
                userEntity.passwordHash(),
                userEntity.isBlocked(),
                userRoles.stream().map(s -> new SimpleGrantedAuthority("ROLE_" + s)).toList()
        );
    }
}
