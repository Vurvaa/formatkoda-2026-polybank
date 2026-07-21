package ru.formatkoda.polybank.ratelimiting;

import org.aspectj.lang.ProceedingJoinPoint;

interface RateLimitKeyResolver {
	RateLimitKey resolve(ProceedingJoinPoint joinPoint, RateLimit rateLimit);
}
