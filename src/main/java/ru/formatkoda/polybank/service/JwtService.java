package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.formatkoda.polybank.security.AuthUserDetailsService;
import ru.formatkoda.polybank.security.JwtHelper;


@Service
@RequiredArgsConstructor
public class JwtService {
    private final AuthenticationManager authenticationManager;

    private final AuthUserDetailsService userDetailsService;

    private final JwtHelper jwtHelper;

    public String generateToken(String login,  String password) {
        this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login, password));

        final UserDetails userDetails =
                userDetailsService.loadUserByUsername(login);

        return jwtHelper.createToken(userDetails.getUsername());
    }
}
