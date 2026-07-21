package ru.formatkoda.polybank.ratelimiting;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.user.UserLogin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
class SecurityContextRateLimitKeyResolver implements RateLimitKeyResolver {

	private static final String KEY_PREFIX = "ratelimit:v1";
	private static final Pattern CUSTOM_KEY_PATTERN = Pattern.compile("[a-zA-Z0-9._-]{1,100}");

	@Override
	public RateLimitKey resolve(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
		Method method = getMethod(joinPoint);
		List<String> keyElements = buildKeyElements(method, rateLimit.key());

		if (rateLimit.perUser())
			addUserKey(keyElements);

		String key = String.join(":", keyElements);

		return new RateLimitKey(key);
	}

	private Method getMethod(ProceedingJoinPoint joinPoint) {
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		Method method = methodSignature.getMethod();

		return AopUtils.getMostSpecificMethod(method, joinPoint.getTarget().getClass());
	}

	private List<String> buildKeyElements(Method method, String keyName) {
		if (keyName.isBlank())
			return keyFromMethod(method);
		return keyFromName(keyName);
	}

	private List<String> keyFromMethod(Method method) {
		String className = method.getDeclaringClass().getName();
		String methodName = method.getName();

		List<String> elements = new ArrayList<>(List.of(KEY_PREFIX, className, methodName));

		if (method.getParameterCount() > 0)
			elements.add(buildParameters(method));

		return elements;
	}

	private List<String> keyFromName(String keyName) {
		if (!CUSTOM_KEY_PATTERN.matcher(keyName).matches())
			throw new IllegalArgumentException("invalid custom rate-limit key: " + keyName);
		return new ArrayList<>(List.of(KEY_PREFIX, keyName));
	}

	private String buildParameters(Method method) {
		List<String> parameters = Arrays.stream(method.getParameterTypes())
				.map(Class::getName)
				.toList();
		return String.join("_", parameters);
	}

	private void addUserKey(List<String> keyElements) {
		keyElements.add("user");
		keyElements.add(buildUserKey());
	}

	private String buildUserKey() {
		Authentication authentication = SecurityContextHolder
				.getContext()
				.getAuthentication();

		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken)
			throw new IllegalStateException("perUser rate limit requires authenticated user");

		Object principal = authentication.getPrincipal();

		if (!(principal instanceof UserLogin(String value)))
			throw new IllegalStateException("unsupported authentication principal");

		if (value.isBlank())
			throw new IllegalStateException("login is empty");

		return value;
	}
}
