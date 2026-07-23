package ru.formatkoda.polybank.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.formatkoda.polybank.config.CurrentUserLogin;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.ratelimiting.RateLimit;

// keep this controller for demonstration purposes
@RestController
@ConditionalOnExpression("${dummy-controller.enabled:false}")
@RateLimit(requests = 100)
@RequestMapping("/test")
public class DummyController {

	@GetMapping("/rps-3-per-user-false")
	@RateLimit(requests = 3)
	public ResponseEntity<String> rps3perUserFalse() {
		return ResponseEntity.ok("foo");
	}

	@GetMapping("/global-rps-100-per-user-false")
	public ResponseEntity<String> globalRps100perUserFalse() {
		return ResponseEntity.ok("bar");
	}

	@SecurityRequirement(name = "oauth2")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/rps-2-per-user-true")
	@RateLimit(requests = 2, perUser = true)
	public ResponseEntity<String> rps2perUserTrue(@CurrentUserLogin UserLogin login, String bar) {
		return ResponseEntity.ok(login.value() + bar);
	}
}
