package ru.formatkoda.polybank.controller;


import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.exception.BusinessLogicException;
import ru.formatkoda.polybank.exception.RateLimitExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
class ApiExceptionHandler {
	@ExceptionHandler(
			value = {
					 MethodArgumentNotValidException.class,
					 HandlerMethodValidationException.class
			}
	)
	ProblemDetail handleBeanValidationException(BindException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");

		problem.setProperty(
				"errors",
				ex.getBindingResult()
						.getFieldErrors()
						.stream()
						.map(
								e -> {
									Map<String, Object> errors = new LinkedHashMap<>();
									errors.put("field", e.getField());
									errors.put("message", e.getDefaultMessage());
									errors.put("code", e.getCode());

									return errors;
								}
						)
						.toList()
		);

		return problem;
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthenticationException(AuthenticationException ex) {
		return buildProblem(
				ex,
				HttpStatus.UNAUTHORIZED,
				"Authentication error",
				Map.of(
						"code", "UNAUTHORIZED"
				)
		);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
		return buildProblem(
				ex,
				HttpStatus.FORBIDDEN,
				"No access rights",
				Map.of(
						"code", "FORBIDDEN"
				)
		);
	}

	@ExceptionHandler(LockedException.class)
	ProblemDetail handleLockedException(LockedException ex) {
		return buildProblem(
				ex,
				HttpStatus.LOCKED,
				"User has been blocked",
				Map.of(
						"code", "LOCKED"
				)
		);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
		return buildProblem(
				ex,
				HttpStatus.NOT_FOUND,
				"Resource doesn't exist",
				Map.of(
						"code", "RESOURCE_NOT_FOUND"
				)
		);
	}

	@ExceptionHandler(BusinessLogicException.class)
	ProblemDetail handleBusinessLogicException(BusinessLogicException ex) {
		return buildProblem(
				ex,
				HttpStatus.CONFLICT,
				"Business logic failed",
				Map.of(
						"code", "BUSINESS_RULE_VIOLATION"
				)
		);
	}

	@ExceptionHandler(RateLimitExceededException.class)
	ResponseEntity<ProblemDetail> handleRateLimitExceededException(RateLimitExceededException ex) {
		ProblemDetail problemDetail = buildProblem(
				ex,
				HttpStatus.TOO_MANY_REQUESTS,
				"Too many requests",
				Map.of(
						"code", "TOO_MANY_REQUESTS"
				)
		);

		return ResponseEntity
				.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, ex.retryAfterSeconds())
				.body(problemDetail);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleGeneralException(Exception ex) {
		return buildProblem(
				ex,
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Server logic failed",
				Map.of(
						"code", "INTERNAL_SERVER_ERROR"
				)
		);
	}

	private ProblemDetail buildProblem(Exception ex, HttpStatus status, String title, Map<String, String> properties) {
		ProblemDetail problem = ProblemDetail.forStatus(status);
		problem.setTitle(title);
		properties.keySet().forEach(key -> problem.setProperty(key, properties.get(key)));
		problem.setProperty("message", ex.getMessage());

		return problem;
	}
}
