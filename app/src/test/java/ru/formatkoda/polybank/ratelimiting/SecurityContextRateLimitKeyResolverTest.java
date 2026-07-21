package ru.formatkoda.polybank.ratelimiting;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.formatkoda.polybank.domain.user.UserLogin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityContextRateLimitKeyResolverTest {

	private ProceedingJoinPoint joinPoint;
	private MethodSignature signature;
	private SecurityContextRateLimitKeyResolver resolver;

	@BeforeEach
	void setUp() {
		joinPoint = mock(ProceedingJoinPoint.class);
		signature = mock(MethodSignature.class);
		resolver = new SecurityContextRateLimitKeyResolver();

		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getTarget()).thenReturn(new TestController());
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldBuildKeyFromMethodWithoutParameters() throws Exception {
		Method method = TestController.class.getDeclaredMethod("withoutParameters");
		givenMethod(method);

		RateLimitKey result = resolver.resolve(joinPoint, rateLimit(false, ""));

		assertEquals(
				"ratelimit:v1:"
						+ TestController.class.getName()
						+ ":withoutParameters",
				result.value()
		);
	}

	@Test
	void shouldIncludeParameterTypesInMethodKey() throws Exception {
		Method method = TestController.class.getDeclaredMethod("withParameters", String.class, Long.class);
		givenMethod(method);

		RateLimitKey result = resolver.resolve(joinPoint, rateLimit(false, ""));

		assertEquals(
				"ratelimit:v1:"
						+ TestController.class.getName()
						+ ":withParameters:"
						+ "java.lang.String_java.lang.Long",
				result.value()
		);
	}

	@Test
	void shouldUseExplicitKeyWhenSpecified() throws Exception {
		Method method = TestController.class.getDeclaredMethod("withoutParameters");
		givenMethod(method);

		RateLimitKey result = resolver.resolve(joinPoint, rateLimit(false, "money-transfer"));

		assertEquals("ratelimit:v1:money-transfer", result.value());
	}

	@Test
	void shouldAddUserKeyWhenPerUserIsEnabled() throws Exception {
		Method method = TestController.class.getDeclaredMethod("withoutParameters");
		givenMethod(method);
		authenticate(new UserLogin("alice"));

		RateLimitKey result = resolver.resolve(joinPoint, rateLimit(true, "money-transfer"));

		assertEquals("ratelimit:v1:money-transfer:user:alice", result.value());
	}

	@Test
	void shouldBuildDifferentKeysForDifferentUsers() throws Exception {
		Method method = TestController.class.getDeclaredMethod("withoutParameters");
		givenMethod(method);
		authenticate(new UserLogin("alice"));

		RateLimitKey aliceKey = resolver.resolve(joinPoint, rateLimit(true, "money-transfer"));
		authenticate(new UserLogin("bob"));
		RateLimitKey bobKey = resolver.resolve(joinPoint, rateLimit(true, "money-transfer"));

		assertEquals("ratelimit:v1:money-transfer:user:alice", aliceKey.value());
		assertEquals("ratelimit:v1:money-transfer:user:bob", bobKey.value());
	}

	@Test
	void shouldRejectInvalidExplicitKey() throws Exception {
		Method method = TestController.class.getDeclaredMethod("withoutParameters");
		givenMethod(method);
		RateLimit rateLimit = rateLimit(false, "money:transfer");

		assertThrows(
				IllegalArgumentException.class,
				() -> resolver.resolve(joinPoint, rateLimit)
		);
	}

	@Test
	void shouldRejectPerUserModeWithoutAuthentication() throws Exception {
		Method method = TestController.class.getDeclaredMethod("withoutParameters");
		givenMethod(method);
		RateLimit rateLimit = rateLimit(true, "");

		assertThrows(
				IllegalStateException.class,
				() -> resolver.resolve(joinPoint, rateLimit)
		);
	}

	@Test
	void shouldRejectAnonymousAuthentication() throws Exception {
		Method method = TestController.class.getDeclaredMethod("withoutParameters");
		givenMethod(method);
		AnonymousAuthenticationToken authentication =
				new AnonymousAuthenticationToken(
						"key",
						"anonymousUser",
						List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
				);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		RateLimit rateLimit = rateLimit(true, "");

		assertThrows(
				IllegalStateException.class,
				() -> resolver.resolve(joinPoint, rateLimit)
		);
	}

	@Test
	void shouldRejectUnsupportedPrincipalType() throws Exception {
		Method method = TestController.class.getDeclaredMethod("withoutParameters");
		givenMethod(method);
		authenticate("alice");

		RateLimit rateLimit = rateLimit(true, "");

		assertThrows(
				IllegalStateException.class,
				() -> resolver.resolve(joinPoint, rateLimit)
		);
	}

	@Test
	void shouldRejectBlankLogin() throws Exception {
		Method method = TestController.class.getDeclaredMethod("withoutParameters");
		givenMethod(method);
		authenticate(new UserLogin(" "));
		RateLimit rateLimit = rateLimit(true, "");

		assertThrows(
				IllegalStateException.class,
				() -> resolver.resolve(joinPoint, rateLimit)
		);
	}

	private void givenMethod(Method method) {
		when(signature.getMethod()).thenReturn(method);
	}

	private void authenticate(Object principal) {
		TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null);
		authentication.setAuthenticated(true);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private RateLimit rateLimit(boolean perUser, String key) {
		return new RateLimit() {
			@Override
			public int requests() {
				return 10;
			}

			@Override
			public long periodMillis() {
				return 1_000;
			}

			@Override
			public boolean perUser() {
				return perUser;
			}

			@Override
			public String key() {
				return key;
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return RateLimit.class;
			}
		};
	}

	@SuppressWarnings("java:S1186")
	private static class TestController {
		void withoutParameters() {
		}

		void withParameters(String value, Long id) {
		}
	}
}