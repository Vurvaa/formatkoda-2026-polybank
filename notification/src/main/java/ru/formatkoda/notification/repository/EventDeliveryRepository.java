package ru.formatkoda.notification.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import ru.formatkoda.notification.domain.DeliveryEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static ru.formatkoda.notification.jooq.generated.Tables.DELIVERY;

@Repository
@RequiredArgsConstructor
public class EventDeliveryRepository {
    private final DSLContext dsl;

    public Optional<DeliveryEntity> save(DeliveryEntity delivery) {
        return dsl
                .insertInto(DELIVERY)
                .set(DELIVERY.EVENT_ID, delivery.eventId())
                .set(DELIVERY.STATUS, delivery.status().name())
                .set(DELIVERY.RETRY_AT, delivery.retryAt())
                .set(DELIVERY.REMAINING_ATTEMPTS, delivery.remainingAttempts())
                .set(DELIVERY.NOTIFICATION_TYPE, delivery.notificationType().name())
                .returning()
                .fetchOptional(this::toEntity);
    }

    public List<DeliveryEntity> save(List<DeliveryEntity> deliveries) {
        return dsl
                .insertInto(DELIVERY,
                        DELIVERY.EVENT_ID,
                        DELIVERY.STATUS,
                        DELIVERY.RETRY_AT,
                        DELIVERY.REMAINING_ATTEMPTS,
                        DELIVERY.NOTIFICATION_TYPE)
                .valuesOfRows(deliveries.stream()
                        .map(d ->
                                DSL.row(
                                        d.eventId(),
                                        d.status().name(),
                                        d.retryAt(),
                                        d.remainingAttempts(),
                                        d.notificationType().name()
                                )
                        )
                        .toList()
                )
                .returning()
                .fetch(this::toEntity);
    }

    public List<DeliveryEntity> updateRetryAt(
            List<Long> deliveryIds,
            OffsetDateTime retryAt
    ) {
        return dsl
                .update(DELIVERY)
                .set(DELIVERY.RETRY_AT, retryAt)
                .where(DELIVERY.ID.in(deliveryIds))
                .returning()
                .fetch(this::toEntity);
    }

    public List<DeliveryEntity> updateStatus(
            List<Long> deliveryIds,
            DeliveryEntity.Status status
    ) {
        return dsl
                .update(DELIVERY)
                .set(DELIVERY.STATUS, status.name())
                .where(DELIVERY.ID.in(deliveryIds))
                .returning()
                .fetch(this::toEntity);
    }

    public List<DeliveryEntity> updateStatusAndRetryAt(
            List<Long> deliveryIds,
            DeliveryEntity.Status status,
            OffsetDateTime retryAt
    ) {
        return dsl
                .update(DELIVERY)
                .set(DELIVERY.STATUS, status.name())
                .set(DELIVERY.RETRY_AT, retryAt)
                .where(DELIVERY.ID.in(deliveryIds))
                .returning()
                .fetch(this::toEntity);
    }

    public List<DeliveryEntity> findActiveByStatusAndRetryAt(
            DeliveryEntity.Status status,
            OffsetDateTime retryAtLimit,
            long limit
    ) {
        return dsl
                .select(DELIVERY)
                .where(DELIVERY.STATUS
                        .eq(status.name())
                        .and(
                                DELIVERY.RETRY_AT
                                        .le(retryAtLimit)
                        )
                )
                .limit(limit)
                .fetch(this::toEntity);
    }

    public DeliveryEntity toEntity(Record r) {
        return new DeliveryEntity(
                r.get(DELIVERY.ID),
                r.get(DELIVERY.EVENT_ID),
                DeliveryEntity.Status.valueOf(r.get(DELIVERY.STATUS)),
                r.get(DELIVERY.RETRY_AT),
                r.get(DELIVERY.REMAINING_ATTEMPTS),
                DeliveryEntity.Type.valueOf(r.get(DELIVERY.NOTIFICATION_TYPE))
        );
    }
}
