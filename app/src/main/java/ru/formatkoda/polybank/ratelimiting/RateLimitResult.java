package ru.formatkoda.polybank.ratelimiting;

import java.time.Duration;

record RateLimitResult(
	boolean allowed,
	long remainingRequests,
	Duration retryAfter
) {
}
