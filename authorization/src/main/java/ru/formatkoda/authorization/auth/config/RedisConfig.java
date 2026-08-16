package ru.formatkoda.authorization.auth.config;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import ru.formatkoda.authorization.auth.service.redis.authorization.RedisAuthorizationService;
import ru.formatkoda.authorization.auth.service.redis.authorization.RedisAuthorizationConsentService;

@Configuration
@EnableRedisRepositories
@RequiredArgsConstructor
public class RedisConfig {
    @Value("${spring.security.oauth2.authorizationserver.redis.ttl}")
    private long ttlInMinutes;

    @Bean
    public RedisTemplate<String, OAuth2Authorization> redisTemplateOAuth2Authorization(
            RedisConnectionFactory redisConnectionFactory
    ) {
        RedisTemplate<String, OAuth2Authorization> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public OAuth2AuthorizationService oAuth2AuthorizationService(
            RedisTemplate<String, OAuth2Authorization> redisTemplate
    ) {
        return new RedisAuthorizationService(redisTemplate, ttlInMinutes);
    }


    @Bean
    public RedisTemplate<String, OAuth2AuthorizationConsent> redisTemplateOAuth2AuthorizationConsent(
            RedisConnectionFactory redisConnectionFactory
    ) {
        RedisTemplate<String, OAuth2AuthorizationConsent> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public OAuth2AuthorizationConsentService oAuth2AuthorizationConsentService(
            RedisTemplate<String, OAuth2AuthorizationConsent> redisTemplate
    ) {
        return new RedisAuthorizationConsentService(
                redisTemplate,
                ttlInMinutes
        );
    }
}