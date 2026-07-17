package ru.formatkoda.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.exception.ResourceNotFoundException;
import ru.formatkoda.notification.messaging.dto.NotificationEventDto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.formatkoda.notification.testutil.NotificationTestData.EMAIL;
import static ru.formatkoda.notification.testutil.NotificationTestData.EMAIL_TYPES;
import static ru.formatkoda.notification.testutil.NotificationTestData.TEMPLATE_NAME;
import static ru.formatkoda.notification.testutil.NotificationTestData.event;
import static ru.formatkoda.notification.testutil.NotificationTestData.notificationEventDto;
import static ru.formatkoda.notification.testutil.NotificationTestData.payloadEntity;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EventService eventService;

    @Mock
    private DeliveryPlanningService deliveryPlanningService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void processNotificationShouldCreateEventAndInitializeDelivery() throws JacksonException {
        NotificationEventDto dto = notificationEventDto(objectMapper);
        EventEntity createdEvent = event();

        when(eventService.createEventOrThrow(org.mockito.ArgumentMatchers.any(EventEntity.class)))
                .thenReturn(createdEvent);

        notificationService.processNotification(dto);

        ArgumentCaptor<EventEntity> eventCaptor = ArgumentCaptor.forClass(EventEntity.class);

        verify(eventService).createEventOrThrow(eventCaptor.capture());

        EventEntity eventToSave = eventCaptor.getValue();
        JsonNode payload = objectMapper.readTree(eventToSave.payload().data());

        assertThat(eventToSave.id()).isNull();
        assertThat(eventToSave.receivedAt()).isNull();
        assertThat(payload.get("notificationTemplateName").asString()).isEqualTo(TEMPLATE_NAME);
        assertThat(payload.get("entity").get("email").asString()).isEqualTo(EMAIL);

        verify(deliveryPlanningService).initializeDeliveryByEventOrThrow(createdEvent, "EMAIL");
    }

    @Test
    void processNotificationShouldThrowExceptionWhenTemplateUnknown() throws JacksonException {
        NotificationEventDto dto = new NotificationEventDto(
                EMAIL_TYPES,
                "UNKNOWN",
                payloadEntity(objectMapper)
        );

        assertThatThrownBy(() -> notificationService.processNotification(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("template not found");

        verifyNoInteractions(eventService, deliveryPlanningService);
    }

    @Test
    void processNotificationShouldThrowExceptionWhenNotificationTypeUnknown() throws JacksonException {
        NotificationEventDto dto = new NotificationEventDto(
                List.of("SMS"),
                TEMPLATE_NAME,
                payloadEntity(objectMapper)
        );

        assertThatThrownBy(() -> notificationService.processNotification(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("type not found");

        verifyNoInteractions(eventService, deliveryPlanningService);
    }
}
