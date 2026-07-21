package ru.formatkoda.polybank.exception;

import java.time.Duration;

public class RateLimitExceededException extends RuntimeException {

	private final Duration retryAfter;

	public RateLimitExceededException(Duration retryAfter) {
		super("rate limit exceeded");
		this.retryAfter = retryAfter;
	}

	public String retryAfterSeconds() {
		long retryAfterSeconds = Math.max(
				1,
				(long) Math.ceil(retryAfter.toMillis() / 1000.0)
		);

		return Long.toString(retryAfterSeconds);
	}
}
