package ru.formatkoda.polybank.ratelimiting;

import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.formatkoda.polybank.exception.RateLimitExceededException;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

	private static final int METHOD_REQUESTS = 10;
	private static final int CLASS_REQUESTS = 20;
	private static final long PERIOD_MILLIS = 2_000;

	private ProceedingJoinPoint joinPoint;
	private MethodSignature signature;
	private RateLimiter rateLimiter;
	private RateLimitKeyResolver keyResolver;
	private HttpServletResponse httpResponse;
	private RateLimitAspect aspect;

	@BeforeEach
	void setUp() {
		joinPoint = mock(ProceedingJoinPoint.class);
		signature = mock(MethodSignature.class);
		rateLimiter = mock(RateLimiter.class);
		keyResolver = mock(RateLimitKeyResolver.class);
		httpResponse = mock(HttpServletResponse.class);

		aspect = new RateLimitAspect(rateLimiter, keyResolver, httpResponse);
		ReflectionTestUtils.setField(aspect, "enabled", true);
		when(joinPoint.getSignature()).thenReturn(signature);
	}

	@Test
	void shouldProceedAndWriteHeadersWhenRequestIsAllowed() throws Throwable {
		MethodAnnotatedController target = new MethodAnnotatedController();
		RateLimit rateLimit = prepareJoinPoint(target, "limited");
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");
		RateLimitResult result = new RateLimitResult(true, 7, Duration.ZERO);
		Object expected = new Object();

		when(keyResolver.resolve(joinPoint, rateLimit)).thenReturn(key);
		when(rateLimiter.tryAcquire(key, METHOD_REQUESTS, Duration.ofMillis(PERIOD_MILLIS))).thenReturn(result);
		when(joinPoint.proceed()).thenReturn(expected);

		Object actual = aspect.limit(joinPoint);

		assertSame(expected, actual);
		verify(httpResponse).setHeader("X-RateLimit-Limit", "10");
		verify(httpResponse).setHeader("X-RateLimit-Remaining", "7");
		verify(joinPoint).proceed();
	}

	@Test
	void shouldThrowAndNotProceedWhenLimitIsExceeded() throws Throwable {
		MethodAnnotatedController target = new MethodAnnotatedController();
		RateLimit rateLimit = prepareJoinPoint(target, "limited");
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");
		Duration retryAfter = Duration.ofMillis(850);
		RateLimitResult result = new RateLimitResult(false, 0, retryAfter);

		when(keyResolver.resolve(joinPoint, rateLimit)).thenReturn(key);
		when(rateLimiter.tryAcquire(key, METHOD_REQUESTS, Duration.ofMillis(PERIOD_MILLIS))).thenReturn(result);

		assertThrows(
				RateLimitExceededException.class,
				() -> aspect.limit(joinPoint)
		);

		verify(httpResponse).setHeader("X-RateLimit-Limit", "10");
		verify(httpResponse).setHeader("X-RateLimit-Remaining", "0");
		verify(joinPoint, never()).proceed();
	}

	@Test
	void shouldNotCallLimiterWhenKeyResolutionFails() throws Throwable {
		MethodAnnotatedController target = new MethodAnnotatedController();
		RateLimit rateLimit = prepareJoinPoint(target, "perUser");

		when(keyResolver.resolve(joinPoint, rateLimit))
				.thenThrow(new IllegalStateException("user is not authenticated"));

		assertThrows(
				IllegalStateException.class,
				() -> aspect.limit(joinPoint)
		);

		verifyNoInteractions(rateLimiter);
		verify(joinPoint, never()).proceed();
	}

	@Test
	void shouldUseClassAnnotationWhenMethodAnnotationIsMissing() throws Throwable {
		ClassAnnotatedController target = new ClassAnnotatedController();
		RateLimit rateLimit = prepareJoinPoint(target, "inheritedLimit");
		RateLimitKey key = new RateLimitKey("ratelimit:v1:class");
		RateLimitResult result = new RateLimitResult(true, 19, Duration.ZERO);

		when(keyResolver.resolve(joinPoint, rateLimit)).thenReturn(key);
		when(rateLimiter.tryAcquire(key, CLASS_REQUESTS, Duration.ofSeconds(1))).thenReturn(result);

		aspect.limit(joinPoint);

		verify(httpResponse).setHeader("X-RateLimit-Limit", "20");
		verify(httpResponse).setHeader("X-RateLimit-Remaining", "19");
		verify(joinPoint).proceed();
	}

	@Test
	void shouldPreferMethodAnnotationOverClassAnnotation() throws Throwable {
		ClassAndMethodAnnotatedController target = new ClassAndMethodAnnotatedController();
		RateLimit rateLimit = prepareJoinPoint(target, "specificLimit");
		RateLimitKey key = new RateLimitKey("ratelimit:v1:method");
		RateLimitResult result = new RateLimitResult(true, 9, Duration.ZERO);

		when(keyResolver.resolve(joinPoint, rateLimit)).thenReturn(key);
		when(rateLimiter.tryAcquire(key, METHOD_REQUESTS, Duration.ofMillis(PERIOD_MILLIS))).thenReturn(result);

		aspect.limit(joinPoint);

		verify(httpResponse).setHeader("X-RateLimit-Limit", "10");
		verify(httpResponse).setHeader("X-RateLimit-Remaining", "9");
		verify(joinPoint).proceed();
	}

	@Test
	void shouldProceedWithoutLimiterWhenDisabled() throws Throwable {
		ReflectionTestUtils.setField(aspect, "enabled", false);
		Object expected = new Object();
		when(joinPoint.proceed()).thenReturn(expected);

		Object actual = aspect.limit(joinPoint);

		assertSame(expected, actual);
		verifyNoInteractions(rateLimiter, keyResolver, httpResponse);
	}

	private RateLimit prepareJoinPoint(Object target, String methodName) throws NoSuchMethodException {
		Method method = target.getClass().getDeclaredMethod(methodName);
		when(joinPoint.getTarget()).thenReturn(target);
		when(signature.getMethod()).thenReturn(method);

		RateLimit methodRateLimit = method.getAnnotation(RateLimit.class);
		if (methodRateLimit != null)
			return methodRateLimit;
		return target.getClass().getAnnotation(RateLimit.class);
	}

	@SuppressWarnings("java:S1186")
	private static class MethodAnnotatedController {
		@RateLimit(requests = METHOD_REQUESTS, periodMillis = PERIOD_MILLIS)
		void limited() {
		}

		@RateLimit(requests = METHOD_REQUESTS, periodMillis = PERIOD_MILLIS, perUser = true)
		void perUser() {
		}
	}

	@SuppressWarnings("java:S1186")
	@RateLimit(requests = CLASS_REQUESTS)
	private static class ClassAnnotatedController {
		void inheritedLimit() {
		}
	}

	@SuppressWarnings("java:S1186")
	@RateLimit(requests = CLASS_REQUESTS)
	private static class ClassAndMethodAnnotatedController {
		@RateLimit(requests = METHOD_REQUESTS, periodMillis = PERIOD_MILLIS)
		void specificLimit() {
		}
	}
}