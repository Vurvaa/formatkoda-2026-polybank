package ru.formatkoda.polybank.dto.account;

import ru.formatkoda.polybank.domain.account.AccountEntity;
import ru.formatkoda.polybank.domain.account.AccountNumber;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountResponseDto(
        AccountNumber number,
        BigDecimal balance,
        AccountEntity.Type type,
        AccountEntity.Status status,
        OffsetDateTime createdAt
) {}
