package ru.formatkoda.polybank.domain;

import java.time.OffsetDateTime;

public record User(
        long id,
        String login,
        String name,
        String lastName,
        String pswdHash,
        OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {
}
