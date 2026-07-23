package ru.formatkoda.polybank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String SECURITY_SCHEME = "oauth2";

    @Bean
    public OpenAPI openApi() {
        OAuthFlow authorizationCodeFlow = new OAuthFlow()
                .authorizationUrl("http://localhost:9091/oauth2/authorize")
                .tokenUrl("http://localhost:9091/oauth2/token")
                .scopes(new Scopes()
                        .addString("openid", "OpenID Connect")
                        .addString("profile", "Профиль пользователя")
                        .addString("api", "Доступ к API")
                );

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows().authorizationCode(authorizationCodeFlow));

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME, securityScheme))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
