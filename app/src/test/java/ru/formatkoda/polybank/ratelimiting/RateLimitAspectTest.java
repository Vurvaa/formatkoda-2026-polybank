package ru.formatkoda.polybank.ratelimiting;

import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.formatkoda.polybank.exception.RateLimitExceededException;

import java.lang.annotation.Annotation;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

	private static final int REQUESTS = 10;
	private static final long PERIOD_MILLIS = 2_000;

	private ProceedingJoinPoint joinPoint;
	private RateLimiter rateLimiter;
	private RateLimitKeyResolver keyResolver;
	private HttpServletResponse httpResponse;
	private RateLimitAspect aspect;

	@BeforeEach
	void setUp() {
		joinPoint = mock(ProceedingJoinPoint.class);
		rateLimiter = mock(RateLimiter.class);
		keyResolver = mock(RateLimitKeyResolver.class);
		httpResponse = mock(HttpServletResponse.class);

		aspect = new RateLimitAspect(rateLimiter, keyResolver, httpResponse);
		ReflectionTestUtils.setField(aspect, "enabled", true);
	}

	@Test
	void shouldProceedAndWriteHeadersWhenRequestIsAllowed() throws Throwable {
		RateLimit rateLimit = rateLimit(false);
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");
		RateLimitResult result = new RateLimitResult(true, 7, Duration.ZERO);
		when(keyResolver.resolve(joinPoint, rateLimit)).thenReturn(key);
		when(rateLimiter.tryAcquire(key, REQUESTS, Duration.ofMillis(PERIOD_MILLIS))).thenReturn(result);
		Object expected = new Object();
		when(joinPoint.proceed()).thenReturn(expected);

		Object actual = aspect.limit(joinPoint, rateLimit);

		assertSame(expected, actual);
		verify(httpResponse).setHeader("X-RateLimit-Limit", "10");
		verify(httpResponse).setHeader("X-RateLimit-Remaining", "7");
		verify(joinPoint).proceed();
	}

	@Test
	void shouldThrowAndNotProceedWhenLimitIsExceeded() throws Throwable {
		RateLimit rateLimit = rateLimit(false);
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");
		Duration retryAfter = Duration.ofMillis(850);
		RateLimitResult result = new RateLimitResult(false, 0, retryAfter);
		when(keyResolver.resolve(joinPoint, rateLimit)).thenReturn(key);
		when(rateLimiter.tryAcquire(key, REQUESTS, Duration.ofMillis(PERIOD_MILLIS))).thenReturn(result);

		assertThrows(
				RateLimitExceededException.class,
				() -> aspect.limit(joinPoint, rateLimit)
		);

		verify(httpResponse).setHeader("X-RateLimit-Limit", "10");
		verify(httpResponse).setHeader("X-RateLimit-Remaining", "0");
		verify(joinPoint, never()).proceed();
	}

	@Test
	void shouldNotCallLimiterWhenKeyResolutionFails() throws Throwable {
		RateLimit rateLimit = rateLimit(true);
		when(keyResolver.resolve(joinPoint, rateLimit))
				.thenThrow(new IllegalStateException("user is not authenticated"));

		assertThrows(
				IllegalStateException.class,
				() -> aspect.limit(joinPoint, rateLimit)
		);

		verifyNoMoreInteractions(rateLimiter);
		verify(joinPoint, never()).proceed();
	}

	private RateLimit rateLimit(boolean perUser) {
		return new RateLimit() {
			@Override
			public int requests() {
				return RateLimitAspectTest.REQUESTS;
			}

			@Override
			public long periodMillis() {
				return RateLimitAspectTest.PERIOD_MILLIS;
			}

			@Override
			public boolean perUser() {
				return perUser;
			}

			@Override
			public String key() {
				return "";
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return RateLimit.class;
			}
		};
	}
}