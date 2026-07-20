package ru.formatkoda.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.sender.NotificationSender;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.formatkoda.notification.testutil.NotificationTestData.EMAIL;
import static ru.formatkoda.notification.testutil.NotificationTestData.EVENT_ID;
import static ru.formatkoda.notification.testutil.NotificationTestData.TEMPLATE_NAME;
import static ru.formatkoda.notification.testutil.NotificationTestData.event;
import static ru.formatkoda.notification.testutil.NotificationTestData.pendingDelivery;

@ExtendWith(MockitoExtension.class)
class DeliveryDispatchWorkerTest {
    @Mock
    private EventService eventService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotificationSenderRegistry notificationSenderRegistry;

    @Mock
    private DeliveryPlanningService deliveryPlanningService;

    @Mock
    private NotificationSender notificationSender;

    @InjectMocks
    private DeliveryDispatchWorker deliveryDispatchWorker;

    @Test
    void dispatchShouldSendNotificationAndCompleteDelivery() throws JacksonException {
        DeliveryEntity delivery = pendingDelivery();
        EventEntity event = event();
        JsonNode payload = objectMapper.readTree(event.payload().data());
        String expectedNotification = NotificationTemplate.renderTemplate(
                TEMPLATE_NAME,
                payload.get("entity")
        );

        when(eventService.findEventByIdOrThrow(EVENT_ID)).thenReturn(event);
        when(notificationSenderRegistry.getNotificationSender(DeliveryEntity.Type.EMAIL))
                .thenReturn(notificationSender);
        when(notificationSender.sendNotification(expectedNotification, EMAIL)).thenReturn(true);

        deliveryDispatchWorker.dispatch(delivery);

        verify(eventService).findEventByIdOrThrow(EVENT_ID);
        verify(notificationSenderRegistry).getNotificationSender(DeliveryEntity.Type.EMAIL);
        verify(notificationSender).sendNotification(expectedNotification, EMAIL);
        verify(deliveryPlanningService).completeDelivery(delivery);
        verify(deliveryPlanningService, never()).rescheduleDeliveryOrMarkFailed(delivery);
    }

    @Test
    void dispatchShouldRescheduleDeliveryWhenSenderReturnsFalse() throws JacksonException {
        DeliveryEntity delivery = pendingDelivery();
        EventEntity event = event();
        JsonNode payload = objectMapper.readTree(event.payload().data());
        String expectedNotification = NotificationTemplate.renderTemplate(
                TEMPLATE_NAME,
                payload.get("entity")
        );

        when(eventService.findEventByIdOrThrow(EVENT_ID)).thenReturn(event);
        when(notificationSenderRegistry.getNotificationSender(DeliveryEntity.Type.EMAIL))
                .thenReturn(notificationSender);
        when(notificationSender.sendNotification(expectedNotification, EMAIL)).thenReturn(false);

        deliveryDispatchWorker.dispatch(delivery);

        verify(notificationSender).sendNotification(expectedNotification, EMAIL);
        verify(deliveryPlanningService).rescheduleDeliveryOrMarkFailed(delivery);
        verify(deliveryPlanningService, never()).completeDelivery(delivery);
    }
}
