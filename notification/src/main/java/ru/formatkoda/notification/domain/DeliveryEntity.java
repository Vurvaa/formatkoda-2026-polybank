package ru.formatkoda.notification.domain;

import lombok.NonNull;
import ru.formatkoda.notification.exception.ResourceNotFoundException;

import java.time.OffsetDateTime;

public record DeliveryEntity(
    Long id,
    Long eventId,
    @NonNull Status status,
    @NonNull OffsetDateTime retryAt,
    @NonNull Short remainingAttempts,
    @NonNull Type notificationType
) {
    public enum Status {
        PENDING,
        PROCESSING,
        SENT,
        FAILED
    }

    public enum Type {
        EMAIL;

        public static Type fromName(String typeName) {
            try {
                return Type.valueOf(typeName);
            } catch (IllegalArgumentException _) {
                throw new ResourceNotFoundException("type not found");
            }
        }
    }
}
