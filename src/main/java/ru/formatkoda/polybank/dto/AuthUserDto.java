package ru.formatkoda.polybank.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthUserDto(
        @NotBlank String token
) {
}
