package ru.formatkoda.authorization.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import ru.formatkoda.authorization.auth.security.CustomAuthenticationProvider;
import ru.formatkoda.authorization.auth.security.CustomUserDetailsService;

import java.util.HashSet;
import java.util.Set;

@EnableWebSecurity
@RequiredArgsConstructor
@Configuration()
public class SecurityConfig {
	private final CustomUserDetailsService userDetailsService;

	@Bean
	public AuthenticationProvider authenticationProvider() {
		return new CustomAuthenticationProvider(userDetailsService);
	}

	@Bean
	OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
		return context -> {
			if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN
					|| OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
				Authentication principal = context.getPrincipal();
				Set<GrantedAuthority> authorities = new HashSet<>(principal.getAuthorities());
				context.getClaims().claim("roles", authorities);
				context.getClaims().claim("login", principal.getName());
			}
		};
	}
}