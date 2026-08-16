package ru.formatkoda.authorization.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import ru.formatkoda.authorization.auth.properties.AuthorizationServerProperties;

import java.time.Duration;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class RegisteredClients {
	private static final String FRONTEND_ALIAS = "frontend";
	private static final String SWAGGER_ALIAS = "swagger";
	private static final String API_SCOPE = "api";

	private final AuthorizationServerProperties properties;

	public RegisteredClient getFrontendClient() {
		return RegisteredClient
				.withId(UUID.randomUUID().toString())
				.clientName(FRONTEND_ALIAS)
				.clientId(FRONTEND_ALIAS)
				.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(properties.getFrontendRedirectUri())
				.postLogoutRedirectUri(properties.getFrontendRedirectUri())
				.scope(OidcScopes.OPENID)
				.scope(OidcScopes.PROFILE)
				.scope(API_SCOPE)
				.clientSettings(ClientSettings.builder()
						.requireProofKey(true)
						.requireAuthorizationConsent(false)
						.build())
				.tokenSettings(tokenSettings())
				.build();
	}

	public RegisteredClient getSwaggerClient() {
		return RegisteredClient
				.withId(UUID.randomUUID().toString())
				.clientName(SWAGGER_ALIAS)
				.clientId(SWAGGER_ALIAS)
				.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(properties.getSwaggerRedirectUri())
				.scope(OidcScopes.OPENID)
				.scope(OidcScopes.PROFILE)
				.scope(API_SCOPE)
				.clientSettings(ClientSettings.builder()
						.requireProofKey(true)
						.requireAuthorizationConsent(false)
						.build())
				.tokenSettings(tokenSettings())
				.build();
	}

	private TokenSettings tokenSettings() {
		return TokenSettings.builder()
				.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
				.accessTokenTimeToLive(Duration.ofMinutes(
						properties.getAccessExpirationMinutes()
				))
				.build();
	}
}
