package ru.formatkoda.authorization.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import ru.formatkoda.authorization.auth.security.CustomAuthenticationProvider;
import ru.formatkoda.authorization.auth.security.CustomUserDetailsService;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Configuration()
public class SecurityConfig {
	private final CustomUserDetailsService userDetailsService;
/*	@Bean
	public AuthenticationProvider authenticationProvider() {
		return new CustomAuthenticationProvider(userDetailsService);
	}


	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) {
		return httpSecurity
				.getSharedObject(AuthenticationManagerBuilder.class)
				.authenticationProvider(authenticationProvider())
				.build();
	}*/

	/*@Bean
	OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
		return context -> {
			if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)
					|| OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
				Authentication principal = context.getPrincipal();
				Set<GrantedAuthority> authorities = new HashSet<>(principal.getAuthorities());
				context.getClaims().claims(claim ->
						claim.put("roles", authorities)
				);
			}
		};
	}*/
}