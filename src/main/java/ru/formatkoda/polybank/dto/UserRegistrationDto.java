package ru.formatkoda.polybank.dto;

import jakarta.validation.constraints.NotBlank;

public record UserRegistrationDto(
        @NotBlank
        String login,
        @NotBlank
        String name,
        @NotBlank
        String lastName,
        @NotBlank
        String password
) {}
