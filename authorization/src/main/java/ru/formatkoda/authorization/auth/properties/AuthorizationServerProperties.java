package ru.formatkoda.authorization.auth.properties;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.security.oauth2.authorizationserver")
public class AuthorizationServerProperties {
	private String issuerUrl;
	private List<String> allowedOrigins = new ArrayList<>();
}
