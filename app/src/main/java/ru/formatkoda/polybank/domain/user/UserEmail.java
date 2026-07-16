package ru.formatkoda.polybank.domain.user;

import jakarta.validation.constraints.Email;

public record UserEmail(
        @Email String value
) {
}
