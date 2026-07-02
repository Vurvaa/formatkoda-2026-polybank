package ru.formatkoda.polybank.dto;

import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateAccountResponseDto(
        Long id,
        AccountNumber number,
        BigDecimal balance,
        AccountEntity.AccountType type,
        AccountEntity.AccountStatus status,
        OffsetDateTime createdAt
) {}
