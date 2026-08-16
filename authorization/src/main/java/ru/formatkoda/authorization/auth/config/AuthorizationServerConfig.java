package ru.formatkoda.authorization.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.formatkoda.authorization.auth.properties.AuthorizationServerProperties;

import java.util.List;

@RequiredArgsConstructor
@Configuration()
@EnableWebSecurity
public class AuthorizationServerConfig {
	private static final String LOGIN_ENDPOINT = "/login";

	private final RegisteredClients registeredClients;
	private final AuthorizationServerProperties authorizationProperties;

	@Bean
	@Order(1)
	public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) {
		http.oauth2AuthorizationServer(authorizationServer -> {
					http.securityMatcher(authorizationServer.getEndpointsMatcher());
					authorizationServer
							.oidc(Customizer.withDefaults());
				})
				.authorizeHttpRequests(authorize ->
						authorize
								.anyRequest().authenticated()
				)
				.cors(Customizer.withDefaults())
				.exceptionHandling(exceptions -> exceptions
						.defaultAuthenticationEntryPointFor(
								new LoginUrlAuthenticationEntryPoint(LOGIN_ENDPOINT),
								new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
						)
				);

		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
		http.authorizeHttpRequests(authorize ->
						authorize
								.requestMatchers(
										"/error",
										"/.well-known/appspecific/**"
								).permitAll()
								.anyRequest().authenticated()
				)
				.formLogin(Customizer.withDefaults());

		return http.build();
	}


	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder()
				.issuer(authorizationProperties.getIssuerUrl())
				.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(authorizationProperties.getAllowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public RegisteredClientRepository registeredClientRepository() {
		RegisteredClient frontendClient = registeredClients.getFrontendClient();

		RegisteredClient swaggerClient = registeredClients.getSwaggerClient();

		return new InMemoryRegisteredClientRepository(
				frontendClient,
				swaggerClient
		);
	}
}
