package ru.formatkoda.polybank.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.formatkoda.polybank.domain.account.AccountEntity;

import java.math.BigDecimal;

public record AccountDetailsResponseDto(
        @NotBlank String number,
        @NotNull BigDecimal balance,
        @NotNull AccountEntity.Type type,
        @NotNull AccountEntity.Status status
) {}
