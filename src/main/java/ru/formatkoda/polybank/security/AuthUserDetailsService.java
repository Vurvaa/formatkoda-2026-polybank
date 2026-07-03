package ru.formatkoda.polybank.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findUserByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("not found user with this login"));

        return new AuthUserDetails(
                userEntity.login(),
                userEntity.passwordHash()
        );
    }
}
