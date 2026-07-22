package ru.formatkoda.polybank.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.formatkoda.polybank.security.AuthUserDetailsService;
import ru.formatkoda.polybank.security.JwtAuthenticationEntryPoint;
import ru.formatkoda.polybank.security.JwtFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
	@Value("#{'${app.cors.allowed-origins}'.split(',')}")
	private List<String> allowedOrigins;

	private final JwtFilter jwtFilter;

	private final AuthUserDetailsService authUserDetailsService;

	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	private final PasswordEncoder passwordEncoder;

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		final DaoAuthenticationProvider daoAuthenticationProvider =
				new DaoAuthenticationProvider(authUserDetailsService);

		daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);

		return daoAuthenticationProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) {
		return httpSecurity
				.getSharedObject(AuthenticationManagerBuilder.class)
				.authenticationProvider(authenticationProvider())
				.build();
	}

	@SuppressWarnings("java:S4502")
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
		httpSecurity
				.cors(cors -> { })
				// csrf is disabled because this stateless api authenticates requests with jwt tokens
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/user/sign-up", "/user/sign-in").permitAll()
						.requestMatchers("/actuator/**").permitAll()
						.requestMatchers("/test/**").permitAll()
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated())
				.sessionManagement(sessionManagement ->
						sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtFilter,
						UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling(exception ->
						exception.authenticationEntryPoint(jwtAuthenticationEntryPoint));


		return httpSecurity.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
