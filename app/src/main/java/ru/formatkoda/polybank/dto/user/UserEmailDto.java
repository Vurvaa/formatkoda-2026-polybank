package ru.formatkoda.polybank.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UserEmailDto(
        @NotBlank String value
) {
}
