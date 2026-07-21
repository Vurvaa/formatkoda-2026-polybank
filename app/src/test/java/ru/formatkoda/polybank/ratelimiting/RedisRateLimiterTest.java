package ru.formatkoda.polybank.ratelimiting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class RedisRateLimiterTest {

	private static final long CURRENT_TIME = 1_750_000_000_000L;

	private StringRedisTemplate redisTemplate;
	private RedisScript<List> script;
	private RedisRateLimiter rateLimiter;

	@BeforeEach
	void setUp() {
		redisTemplate = mock(StringRedisTemplate.class);
		script = mock(RedisScript.class);
		Clock clock = Clock.fixed(Instant.ofEpochMilli(CURRENT_TIME), ZoneOffset.UTC);
		rateLimiter = new RedisRateLimiter(redisTemplate, script, clock);
	}

	@Test
	void shouldExecuteScriptAndReturnAllowedResult() {
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");
		when(redisTemplate.execute(
				script,
				List.of(key.value()),
				"10",
				"1000",
				Long.toString(CURRENT_TIME)
		)).thenReturn(List.of(1L, 7L, 0L));

		RateLimitResult result = rateLimiter.tryAcquire(key, 10, Duration.ofSeconds(1));

		assertTrue(result.allowed());
		assertEquals(7, result.remainingRequests());
		assertEquals(Duration.ZERO, result.retryAfter());
		verify(redisTemplate).execute(
				script,
				List.of("ratelimit:v1:test"),
				"10",
				"1000",
				Long.toString(CURRENT_TIME)
		);
	}

	@Test
	void shouldReturnRetryAfterWhenRequestIsRejected() {
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");
		when(redisTemplate.execute(
				script,
				List.of(key.value()),
				"10",
				"1000",
				Long.toString(CURRENT_TIME)
		)).thenReturn(List.of(0L, 0L, 650L));

		RateLimitResult result = rateLimiter.tryAcquire(key, 10, Duration.ofSeconds(1));

		assertFalse(result.allowed());
		assertEquals(0, result.remainingRequests());
		assertEquals(Duration.ofMillis(650), result.retryAfter());
	}

	@Test
	void shouldRejectNonPositiveRequests() {
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");
		Duration duration = Duration.ofSeconds(1);

		assertThrows(
				IllegalArgumentException.class,
				() -> rateLimiter.tryAcquire(key, 0, duration)
		);
	}

	@Test
	void shouldRejectZeroPeriod() {
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");

		assertThrows(
				IllegalArgumentException.class,
				() -> rateLimiter.tryAcquire(key, 10, Duration.ZERO)
		);
	}

	@Test
	void shouldRejectNegativePeriod() {
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");
		Duration duration = Duration.ofMillis(-1);

		assertThrows(
				IllegalArgumentException.class,
				() -> rateLimiter.tryAcquire(key, 10, duration)
		);
	}

	@Test
	void shouldRejectNullRedisResult() {
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");
		when(redisTemplate.execute(
				script,
				List.of(key.value()),
				"10",
				"1000",
				Long.toString(CURRENT_TIME)
		)).thenReturn(null);
		Duration duration = Duration.ofSeconds(1);

		assertThrows(
				IllegalStateException.class,
				() -> rateLimiter.tryAcquire(key, 10, duration)
		);
	}

	@Test
	void shouldRejectRedisResultWithUnexpectedSize() {
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");
		when(redisTemplate.execute(
				script,
				List.of(key.value()),
				"10",
				"1000",
				Long.toString(CURRENT_TIME)
		)).thenReturn(List.of(1L, 9L));
		Duration duration = Duration.ofSeconds(1);

		assertThrows(
				IllegalStateException.class,
				() -> rateLimiter.tryAcquire(key, 10, duration)
		);
	}

	@Test
	void shouldRejectUnexpectedLuaResultType() {
		RateLimitKey key = new RateLimitKey("ratelimit:v1:test");
		when(redisTemplate.execute(
				script,
				List.of(key.value()),
				"10",
				"1000",
				Long.toString(CURRENT_TIME)
		)).thenReturn(List.of("true", 9L, 0L));
		Duration duration = Duration.ofSeconds(1);

		assertThrows(
				IllegalStateException.class,
				() -> rateLimiter.tryAcquire(key, 10, duration)
		);
	}
}