package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.jooq.JSONB;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.messaging.dto.NotificationEventDto;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final ObjectMapper objectMapper;

    private final EventService eventService;
    private final DeliveryPlanningService deliveryPlanningService;

    @Transactional
    public void processNotification(NotificationEventDto notificationEventDto) {
        validateDto(notificationEventDto);

        EventEntity event = new EventEntity(
                null,
                notificationEventDto.userId(),
                JSONB.jsonb(objectMapper.writeValueAsString(notificationEventDto)),
                null
        );
        EventEntity createdEvent = eventService.createEventOrThrow(event);

        notificationEventDto
                .notificationTypeNames()
                .forEach(
                        notificationTypeName ->
                                deliveryPlanningService
                                        .initializeDeliveryByEventOrThrow(
                                                createdEvent,
                                                notificationTypeName
                                        )
                );
    }

    private void validateDto(NotificationEventDto notificationEventDto) {
        NotificationTemplate.fromName(notificationEventDto.notificationTemplateName());

        notificationEventDto.notificationTypeNames().forEach(DeliveryEntity.Type::fromName);
    }
}
