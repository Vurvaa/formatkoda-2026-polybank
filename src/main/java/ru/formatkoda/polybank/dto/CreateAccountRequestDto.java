package ru.formatkoda.polybank.dto;

import jakarta.validation.constraints.NotNull;
import ru.formatkoda.polybank.domain.account.AccountEntity;

public record CreateAccountRequestDto(
        @NotNull AccountEntity.AccountType accountType
) {}
