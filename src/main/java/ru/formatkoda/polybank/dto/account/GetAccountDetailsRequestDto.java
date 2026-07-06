package ru.formatkoda.polybank.dto.account;

import jakarta.validation.constraints.NotBlank;

public record GetAccountDetailsRequestDto(
        @NotBlank String accountNumber
) {}
