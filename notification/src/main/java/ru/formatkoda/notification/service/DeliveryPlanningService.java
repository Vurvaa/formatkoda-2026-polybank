package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.domain.EventEntity;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class DeliveryPlanningService {
    private static final Short TOTAL_ATTEMPTS_TO_DELIVER = 5;
    private static final Duration RETRY_TIME_DELTA = Duration.ofSeconds(10);

    private final EventDeliveryService eventDeliveryService;

    public void initializeDeliveryByEventOrThrow(EventEntity createdEvent, String notificationTypeName) {
        DeliveryEntity delivery = new DeliveryEntity(
                null,
                createdEvent.id(),
                DeliveryEntity.Status.PENDING,
                createdEvent.receivedAt().plus(RETRY_TIME_DELTA),
                TOTAL_ATTEMPTS_TO_DELIVER,
                DeliveryEntity.Type.valueOf(notificationTypeName)
        );
        eventDeliveryService.createDeliveryOrThrow(delivery);
    }
}
