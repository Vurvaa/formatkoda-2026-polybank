package ru.formatkoda.polybank.config;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(
		expression = "new ru.formatkoda.polybank.domain.user.UserLogin(#this.getClaimAsString('sub'))",
		errorOnInvalidType = true
)
public @interface CurrentUserLogin {
}
