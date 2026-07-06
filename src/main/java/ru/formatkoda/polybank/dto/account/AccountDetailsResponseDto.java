package ru.formatkoda.polybank.dto.account;

import jakarta.validation.constraints.NotNull;
import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;

import java.math.BigDecimal;

public record AccountDetailsResponseDto(
        @NotNull AccountNumber number,
        @NotNull BigDecimal balance,
        @NotNull AccountEntity.Type type,
        @NotNull AccountEntity.Status status
) {}
