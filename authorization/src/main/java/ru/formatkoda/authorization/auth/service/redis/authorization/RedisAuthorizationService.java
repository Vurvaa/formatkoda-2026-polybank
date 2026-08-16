package ru.formatkoda.authorization.auth.service.redis.authorization;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.util.Assert;

public class RedisAuthorizationService implements OAuth2AuthorizationService {

    private static final String COMPLETE_KEY_PREFIX = "oauth2_authorization_complete:";

    private static final  String INIT_KEY_PREFIX = "oauth2_authorization_init:";

    private final RedisTemplate<String, OAuth2Authorization> redisTemplate;
    private final ValueOperations<String, OAuth2Authorization> authorizations;
    private final long ttl;

    public RedisAuthorizationService(RedisTemplate<String, OAuth2Authorization> redisTemplate, long ttl) {
        this.redisTemplate = redisTemplate;
        this.authorizations = redisTemplate.opsForValue();
        this.ttl = ttl;
    }

    @Override
    public void save(@NonNull OAuth2Authorization authorization) {
        String key;
        if (isComplete(authorization)) {
            key = COMPLETE_KEY_PREFIX + authorization.getId();

            String initKey = INIT_KEY_PREFIX + authorization.getId();
            if (redisTemplate.hasKey(initKey))
                redisTemplate.delete(initKey);

        } else {
            key = INIT_KEY_PREFIX + authorization.getId();
        }

        authorizations.set(key, authorization, ttl, TimeUnit.MINUTES);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        String key;
        if (isComplete(authorization)) {
            key = COMPLETE_KEY_PREFIX + authorization.getId();
        } else {
            key = INIT_KEY_PREFIX + authorization.getId();
        }
        this.redisTemplate.delete(key);
    }

    @Nullable
    @Override
    public OAuth2Authorization findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        OAuth2Authorization completeAuthorization = authorizations.get(COMPLETE_KEY_PREFIX + id);
        return completeAuthorization != null
            ? completeAuthorization
            : authorizations.get(INIT_KEY_PREFIX + id);
    }

    @Nullable
    @Override
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");
        OAuth2Authorization authorization = findByToken(token, tokenType, COMPLETE_KEY_PREFIX);

        if (authorization == null)
            authorization = findByToken(token, tokenType, INIT_KEY_PREFIX);

        return authorization;
    }

    private OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType, String prefixKey) {
        Set<String> allInitKeys = redisTemplate.keys(prefixKey + "*");
        if (allInitKeys != null) {
            for (String authorizationKey : allInitKeys) {
                OAuth2Authorization authorization = authorizations.get(authorizationKey);
                if (hasToken(authorization, token, tokenType)) {
                    return authorization;
                }
            }
        }
        return null;
    }

    private static boolean isComplete(OAuth2Authorization authorization) {
        return authorization.getAccessToken() != null;
    }

    private static boolean hasToken(
        OAuth2Authorization authorization,
        String token,
        @Nullable OAuth2TokenType tokenType
    ) {
        if (tokenType == null) {
            return matchesState(authorization, token) ||
                    matchesAuthorizationCode(authorization, token) ||
                    matchesAccessToken(authorization, token) ||
                    matchesIdToken(authorization, token) ||
                    matchesRefreshToken(authorization, token);
        }

        return switch (tokenType.getValue()) {
            case OAuth2ParameterNames.STATE -> matchesState(authorization, token);
            case OAuth2ParameterNames.CODE -> matchesAuthorizationCode(authorization, token);
            case "access_token" -> matchesAccessToken(authorization, token);
            case OidcParameterNames.ID_TOKEN -> matchesIdToken(authorization, token);
            case "refresh_token" -> matchesRefreshToken(authorization, token);
            default -> false;
        };
    }

    private static boolean matchesState(OAuth2Authorization authorization, String token) {
        return token.equals(authorization.getAttribute(OAuth2ParameterNames.STATE));
    }

    private static boolean matchesAuthorizationCode(OAuth2Authorization authorization, String token) {
        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
            authorization.getToken(OAuth2AuthorizationCode.class);
        return authorizationCode != null && authorizationCode.getToken().getTokenValue().equals(token);
    }

    private static boolean matchesAccessToken(OAuth2Authorization authorization, String token) {
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
            authorization.getToken(OAuth2AccessToken.class);
        return accessToken != null && accessToken.getToken().getTokenValue().equals(token);
    }

    private static boolean matchesRefreshToken(OAuth2Authorization authorization, String token) {
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
            authorization.getToken(OAuth2RefreshToken.class);
        return refreshToken != null && refreshToken.getToken().getTokenValue().equals(token);
    }

    private static boolean matchesIdToken(OAuth2Authorization authorization, String token) {
        OAuth2Authorization.Token<OidcIdToken> idToken =
            authorization.getToken(OidcIdToken.class);
        return idToken != null && idToken.getToken().getTokenValue().equals(token);
    }
}