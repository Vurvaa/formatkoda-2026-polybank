package ru.formatkoda.polybank.security;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;


@RequiredArgsConstructor
public class AuthUserDetails implements UserDetails {
    private final String login;
    private final String password;
    private final boolean isBlocked;
    private final Collection<? extends GrantedAuthority> authorities;

    @Override
    public boolean isAccountNonLocked() {
        return !isBlocked;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return login;
    }
}
