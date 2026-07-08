package ru.formatkoda.polybank.domain.account;

import lombok.NonNull;

import java.time.OffsetDateTime;

public record AccountInfo(
        @NonNull AccountNumber number,
        @NonNull AccountEntity.Type type,
        @NonNull AccountEntity.Status status,
        @NonNull OffsetDateTime createdAt
) {
}
