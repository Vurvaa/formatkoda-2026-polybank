package ru.formatkoda.polybank.dto;

public record UserRegistrationDto(
        String login,
        String name,
        String lastName,
        String password
) {
}
