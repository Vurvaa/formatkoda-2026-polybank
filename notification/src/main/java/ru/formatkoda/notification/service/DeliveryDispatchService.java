package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.sender.EmailNotificationSender;
import ru.formatkoda.notification.sender.NotificationSender;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class DeliveryDispatchService {
    private final ObjectMapper objectMapper;

    private final EventDeliveryService eventDeliveryService;
    private final EventService eventService;
    private final EmailNotificationSender emailNotificationSender;

    private final NotificationSenderRegistry notificationSenderRegistry;

    private static final long NUMBER_DELIVERIES_TO_DISPATCH = 20L;

    @Scheduled(
            initialDelayString = "${notificaton.delivery.initial-delay-ms:10000}",
            fixedDelayString = "${notification.delivery.fixed-delay-ms:5000}"
    )
    public void dispatchPendingDeliveries() {
        List<DeliveryEntity> deliveriesToDispatch = eventDeliveryService.findDeliveriesWithStatusRetryAtAndLimit(
                DeliveryEntity.Status.PENDING,
                OffsetDateTime.now(ZoneOffset.UTC),
                NUMBER_DELIVERIES_TO_DISPATCH
        );

        deliveriesToDispatch.forEach(this::dispatchDelivery);
    }

    private void dispatchDelivery(DeliveryEntity delivery) {
        EventEntity event = eventService.findEventByIdOrThrow(delivery.eventId());
        JsonNode payload = objectMapper.readTree(
                event.payload().data()
        );

        NotificationSender sender = notificationSenderRegistry.getNotificationSender(delivery.notificationType());
        boolean result = sender.sendNotification(
                NotificationTemplate.renderTemplate(
                        payload.get("notificationTemplateName").asString(),
                        payload.get("entity")
                ),

                "notifications@polybank.ru"
        );
    }
}
