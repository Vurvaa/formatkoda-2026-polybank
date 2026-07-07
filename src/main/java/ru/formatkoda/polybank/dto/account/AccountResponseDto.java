package ru.formatkoda.polybank.dto.account;

import ru.formatkoda.polybank.domain.account.AccountEntity;

import java.time.OffsetDateTime;

public record AccountResponseDto(
        String number,
        String balance,
        AccountEntity.Type type,
        AccountEntity.Status status,
        OffsetDateTime createdAt
) {}
