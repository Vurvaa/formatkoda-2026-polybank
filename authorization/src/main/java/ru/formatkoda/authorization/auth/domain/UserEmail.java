package ru.formatkoda.authorization.auth.domain;

import jakarta.validation.constraints.Email;

public record UserEmail(
        @Email String value
) {
}
