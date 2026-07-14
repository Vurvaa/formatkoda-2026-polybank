package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.jooq.JSONB;
import org.springframework.stereotype.Service;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.messaging.dto.NotificationEventDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final ObjectMapper objectMapper;

    private final EventService eventService;
    private final DeliveryPlanningService deliveryPlanningService;

    public void processNotification(NotificationEventDto notificationEventDto) {
        EventEntity event = new EventEntity(
                null,
                notificationEventDto.userId(),
                JSONB.jsonb(objectMapper.writeValueAsString(notificationEventDto)),
                null
        );
        EventEntity createdEvent = eventService.createEventOrThrow(event);

        deliveryPlanningService.initializeDeliveryByEventOrThrow(createdEvent, notificationEventDto.notificationTypeName());

        /*
        String message = NotificationTemplate.renderTemplate(notificationEventDto.notificationTemplateName(), notificationEventDto.entity());
        System.out.println(message);
         */
    }
}
