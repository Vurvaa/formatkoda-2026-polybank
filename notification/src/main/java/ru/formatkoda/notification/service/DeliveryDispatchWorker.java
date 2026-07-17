package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.sender.NotificationSender;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DeliveryDispatchWorker {

    private final EventService eventService;

    private final ObjectMapper objectMapper;

    private final NotificationSenderRegistry notificationSenderRegistry;

    private final DeliveryPlanningService deliveryPlanningService;

    @Transactional
    public void dispatch(DeliveryEntity delivery) {
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
