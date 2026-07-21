package ru.formatkoda.polybank.ratelimiting;

import lombok.NonNull;

record RateLimitKey(@NonNull String value) {
	RateLimitKey {
		if (value.isBlank())
			throw new IllegalArgumentException("rate-limit key must not be blank");
	}
}
