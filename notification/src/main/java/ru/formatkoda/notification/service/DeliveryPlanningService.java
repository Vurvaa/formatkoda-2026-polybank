package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.domain.EventEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryPlanningService {
    private final ObjectMapper objectMapper;

    private final EventDeliveryService eventDeliveryService;

    private static final Short TOTAL_ATTEMPTS_TO_DELIVER = 5;
    private static final Duration RETRY_TIME_DELTA = Duration.ofSeconds(10);

    @Transactional
    public void initializeDeliveryByEventOrThrow(
            EventEntity createdEvent,
            String notificationTypeName
    ) {
        JsonNode payloadEntity =  objectMapper.readTree(createdEvent.payload().data()).get("entity");
        if (payloadEntity.get(notificationTypeName.toLowerCase()) == null) {
            DeliveryEntity delivery = new DeliveryEntity(
                    null,
                    createdEvent.id(),
                    DeliveryEntity.Status.FAILED,
                    createdEvent.receivedAt(),
                    (short) 0,
                    DeliveryEntity.Type.fromName(notificationTypeName)
            );
            eventDeliveryService.createDeliveryOrThrow(delivery);

            return;
        }

        DeliveryEntity delivery = new DeliveryEntity(
                null,
                createdEvent.id(),
                DeliveryEntity.Status.PENDING,
                createdEvent.receivedAt().plus(RETRY_TIME_DELTA),
                TOTAL_ATTEMPTS_TO_DELIVER,
                DeliveryEntity.Type.fromName(notificationTypeName)
        );
        eventDeliveryService.createDeliveryOrThrow(delivery);
    }

    @Transactional
    public void rescheduleDeliveryOrMarkFailed(DeliveryEntity deliveryEntity) {
        if (deliveryEntity.remainingAttempts() <= 1) {
            eventDeliveryService.updateStatusAndRemainingAttempts(
                    deliveryEntity.id(),
                    DeliveryEntity.Status.FAILED,
                    (short) 0
            );

            return;
        }

        eventDeliveryService.updateStatusRetryAtAndRemainingAttempts(
                deliveryEntity.id(),
                DeliveryEntity.Status.PENDING,
                deliveryEntity.retryAt().plus(RETRY_TIME_DELTA),
                (short) (deliveryEntity.remainingAttempts() - 1)
        );
    }

    @Transactional
    public void completeDelivery(DeliveryEntity deliveryEntity) {
        eventDeliveryService.updateStatusAndRemainingAttempts(
                deliveryEntity.id(),
                DeliveryEntity.Status.SENT,
                (short) (deliveryEntity.remainingAttempts() - 1)
        );
    }
}
