package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.sender.NotificationSender;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryDispatchService {
    private final ObjectMapper objectMapper;

    private final EventDeliveryService eventDeliveryService;
    private final EventService eventService;
    private final DeliveryPlanningService deliveryPlanningService;

    private final NotificationSenderRegistry notificationSenderRegistry;

    private static final long NUMBER_DELIVERIES_TO_DISPATCH = 20L;

    @Scheduled(
            initialDelayString = "${notification.delivery.initial-delay-ms:10000}",
            fixedDelayString = "${notification.delivery.fixed-delay-ms:5000}"
    )
    @Transactional
    public void dispatchPendingDeliveries() {
        List<DeliveryEntity> deliveriesToDispatch = eventDeliveryService
                .findDeliveriesWithStatusRetryAtAndLimit(
                        DeliveryEntity.Status.PENDING,
                        OffsetDateTime.now(ZoneOffset.UTC),
                        NUMBER_DELIVERIES_TO_DISPATCH
        );

        deliveriesToDispatch.forEach(this::dispatchDelivery);
    }

    private void dispatchDelivery(DeliveryEntity delivery) {
        eventDeliveryService.updateStatusOrThrow(
                delivery.id(),
                DeliveryEntity.Status.PROCESSING
        );

        EventEntity event = eventService.findEventByIdOrThrow(delivery.eventId());
        JsonNode payload = objectMapper.readTree(
                event.payload().data()
        );
        JsonNode payloadEntity = payload.get("entity");

        NotificationSender sender = notificationSenderRegistry.getNotificationSender(delivery.notificationType());
        boolean result = sender.sendNotification(
                NotificationTemplate.renderTemplate(
                        payload.get("notificationTemplateName").asString(),
                        payloadEntity
                ),
                payloadEntity.get(
                        delivery
                                .notificationType()
                                .name()
                                .toLowerCase()
                ).asString()
        );

        if (!result) {
            deliveryPlanningService.rescheduleDeliveryOrMarkFailed(delivery);
            return;
        }

        deliveryPlanningService.completeDelivery(delivery);
    }
}
