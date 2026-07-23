package ru.formatkoda.authorization.auth.security;


import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {
	private final CustomUserDetailsService customUserDetailsService;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		if (authentication instanceof OidcUserInfoAuthenticationToken oidcUserInfoAuthenticationToken) {
			JwtAuthenticationToken principal =
					(JwtAuthenticationToken) oidcUserInfoAuthenticationToken.getPrincipal();

			return new OidcUserInfoAuthenticationToken(
					oidcUserInfoAuthenticationToken, new OidcUserInfo(principal.getToken().getClaims())
			);
		}

		UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;
		customUserDetailsService.loadUserByUsername(auth.getName());

		return new UsernamePasswordAuthenticationToken(auth.getName(), auth.getCredentials(), auth.getAuthorities());
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication)
				|| OidcUserInfoAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
