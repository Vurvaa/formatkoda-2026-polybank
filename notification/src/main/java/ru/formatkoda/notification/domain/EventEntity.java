package ru.formatkoda.notification.domain;

import lombok.NonNull;
import org.jooq.JSONB;

import java.time.OffsetDateTime;

public record EventEntity(
        Long id,
        @NonNull Long userId,
        @NonNull JSONB payload,
        OffsetDateTime receivedAt
) {}
