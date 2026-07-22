package ru.formatkoda.polybank.ratelimiting;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.exception.RateLimitExceededException;

import java.lang.reflect.Method;
import java.time.Duration;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
class RateLimitAspect {

	private static final String LIMIT_HEADER = "X-RateLimit-Limit";
	private static final String REMAINING_HEADER = "X-RateLimit-Remaining";

	@Value("${ratelimit.enabled}")
	private boolean enabled;

	private final RateLimiter rateLimiter;
	private final RateLimitKeyResolver keyResolver;
	private final HttpServletResponse httpResponse;

	@Around("@within(RateLimit) || @annotation(RateLimit)")
	Object limit(ProceedingJoinPoint joinPoint) throws Throwable {
		if (!enabled)
			return joinPoint.proceed();

		RateLimit rateLimit = resolveRateLimit(joinPoint);
		log.debug("got annotation: {} {}", rateLimit.requests(), rateLimit.perUser());
		Duration period = Duration.ofMillis(rateLimit.periodMillis());
		RateLimitKey key = keyResolver.resolve(joinPoint, rateLimit);

		RateLimitResult result = rateLimiter.tryAcquire(key, rateLimit.requests(), period);

		httpResponse.setHeader(LIMIT_HEADER, Long.toString(rateLimit.requests()));
		httpResponse.setHeader(REMAINING_HEADER, Long.toString(result.remainingRequests()));

		if (!result.allowed())
			throw new RateLimitExceededException(result.retryAfter());

		return joinPoint.proceed();
	}

	private RateLimit resolveRateLimit(ProceedingJoinPoint joinPoint) {
		Class<?> targetClass = AopUtils.getTargetClass(joinPoint.getTarget());
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), targetClass);

		RateLimit methodRateLimit = AnnotatedElementUtils.findMergedAnnotation(method, RateLimit.class);
		if (methodRateLimit != null)
			return methodRateLimit;

		RateLimit classRateLimit = AnnotatedElementUtils.findMergedAnnotation(targetClass, RateLimit.class);
		if (classRateLimit != null)
			return classRateLimit;

		throw new IllegalStateException("RateLimit annotation was not found");
	}
}
