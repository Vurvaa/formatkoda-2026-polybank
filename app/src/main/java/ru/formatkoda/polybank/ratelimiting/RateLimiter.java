package ru.formatkoda.polybank.ratelimiting;

import java.time.Duration;

interface RateLimiter {
	RateLimitResult tryAcquire(RateLimitKey rateLimitKey, int requests, Duration period);
}
