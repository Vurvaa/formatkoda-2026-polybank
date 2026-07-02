package ru.formatkoda.polybank.domain.account;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

public record AccountEntity (
    Long id,
    AccountNumber number,
    Long userId,
    BigDecimal balance,
    AccountType type,
    AccountStatus status,
    OffsetDateTime createdAt
) {

    public static final String ACCOUNT_NUMBER_PREFIX = "6767";

    public AccountEntity {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(number);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(balance);
        Objects.requireNonNull(type);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
    }

    public enum AccountStatus {
        ACTIVE,
        FROZEN,
        BLOCKED,
        CLOSED
    }

    public enum AccountType {
        CURRENT,
        FIXED_DEPOSIT,
        SAVINGS,
        CREDIT
    }
}
