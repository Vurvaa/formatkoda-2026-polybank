package ru.formatkoda.polybank.ratelimiting;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("rawtypes")
class RedisRateLimiter implements RateLimiter {

	private final StringRedisTemplate redisTemplate;
	private final RedisScript<List> rateLimitScript;
	private final Clock clock;

	@Override
	public RateLimitResult tryAcquire(RateLimitKey rateLimitKey, int requests, Duration period) {
		validate(requests, period);

		log.trace("executing rate limiter for key={}", rateLimitKey.value());

		List<?> result = redisTemplate.execute(
				rateLimitScript,
				List.of(rateLimitKey.value()),
				Long.toString(requests),
				Long.toString(period.toMillis()),
				Long.toString(clock.millis())
		);

		if (result == null || result.size() != 3)
			throw new IllegalStateException("redis rate-limit script returned invalid result");

		return buildRateLimitResult(result);
	}

	private void validate(long requests, Duration period) {
		if (requests <= 0)
			throw new IllegalArgumentException("requests must be positive");

		if (period == null || period.isZero() || period.isNegative())
			throw new IllegalArgumentException("period must be positive");
	}

	private RateLimitResult buildRateLimitResult(List<?> redisResult) {
		boolean allowed = toLong(redisResult.get(0)) == 1;
		long remainingRequests = toLong(redisResult.get(1));
		long retryAfter = toLong(redisResult.get(2));

		return new RateLimitResult(allowed, remainingRequests, Duration.ofMillis(retryAfter));
	}

	private long toLong(@NonNull Object value) {
		if (value instanceof Number number)
			return number.longValue();
		throw new IllegalStateException("unexpected Lua result value: " + value);
	}
}
