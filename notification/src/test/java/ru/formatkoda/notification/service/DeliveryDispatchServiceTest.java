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

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.formatkoda.notification.testutil.NotificationTestData.DELIVERY_ID;
import static ru.formatkoda.notification.testutil.NotificationTestData.EMAIL;
import static ru.formatkoda.notification.testutil.NotificationTestData.EVENT_ID;
import static ru.formatkoda.notification.testutil.NotificationTestData.TEMPLATE_NAME;
import static ru.formatkoda.notification.testutil.NotificationTestData.event;
import static ru.formatkoda.notification.testutil.NotificationTestData.pendingDelivery;

@ExtendWith(MockitoExtension.class)
class DeliveryDispatchServiceTest {
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EventDeliveryService eventDeliveryService;

    @Mock
    private EventService eventService;

    @Mock
    private DeliveryPlanningService deliveryPlanningService;

    @Mock
    private NotificationSenderRegistry notificationSenderRegistry;

    @Mock
    private NotificationSender notificationSender;

    @InjectMocks
    private DeliveryDispatchService deliveryDispatchService;

    @Test
    void dispatchPendingDeliveriesShouldSendNotificationAndCompleteDelivery() throws JacksonException {
        DeliveryEntity delivery = pendingDelivery();
        EventEntity event = event();
        JsonNode payload = objectMapper.readTree(event.payload().data());
        String expectedNotification = NotificationTemplate.renderTemplate(
                TEMPLATE_NAME,
                payload.get("entity")
        );

        when(eventDeliveryService.findDeliveriesWithStatusRetryAtAndLimit(
                eq(DeliveryEntity.Status.PENDING),
                any(OffsetDateTime.class),
                eq(20L)
        )).thenReturn(List.of(delivery));
        when(eventService.findEventByIdOrThrow(EVENT_ID)).thenReturn(event);
        when(notificationSenderRegistry.getNotificationSender(DeliveryEntity.Type.EMAIL))
                .thenReturn(notificationSender);
        when(notificationSender.sendNotification(expectedNotification, EMAIL)).thenReturn(true);

        deliveryDispatchService.dispatchPendingDeliveries();

        verify(eventDeliveryService).findDeliveriesWithStatusRetryAtAndLimit(
                eq(DeliveryEntity.Status.PENDING),
                any(OffsetDateTime.class),
                eq(20L)
        );
        verify(eventDeliveryService).updateStatusOrThrow(DELIVERY_ID, DeliveryEntity.Status.PROCESSING);
        verify(eventService).findEventByIdOrThrow(EVENT_ID);
        verify(notificationSenderRegistry).getNotificationSender(DeliveryEntity.Type.EMAIL);
        verify(notificationSender).sendNotification(expectedNotification, EMAIL);
        verify(deliveryPlanningService).completeDelivery(delivery);
        verify(deliveryPlanningService, never()).rescheduleDeliveryOrMarkFailed(delivery);
    }

    @Test
    void dispatchPendingDeliveriesShouldRescheduleDeliveryWhenSenderReturnsFalse() throws JacksonException {
        DeliveryEntity delivery = pendingDelivery();
        EventEntity event = event();
        JsonNode payload = objectMapper.readTree(event.payload().data());
        String expectedNotification = NotificationTemplate.renderTemplate(
                TEMPLATE_NAME,
                payload.get("entity")
        );

        when(eventDeliveryService.findDeliveriesWithStatusRetryAtAndLimit(
                eq(DeliveryEntity.Status.PENDING),
                any(OffsetDateTime.class),
                eq(20L)
        )).thenReturn(List.of(delivery));
        when(eventService.findEventByIdOrThrow(EVENT_ID)).thenReturn(event);
        when(notificationSenderRegistry.getNotificationSender(DeliveryEntity.Type.EMAIL))
                .thenReturn(notificationSender);
        when(notificationSender.sendNotification(expectedNotification, EMAIL)).thenReturn(false);

        deliveryDispatchService.dispatchPendingDeliveries();

        verify(eventDeliveryService).updateStatusOrThrow(DELIVERY_ID, DeliveryEntity.Status.PROCESSING);
        verify(notificationSender).sendNotification(expectedNotification, EMAIL);
        verify(deliveryPlanningService).rescheduleDeliveryOrMarkFailed(delivery);
        verify(deliveryPlanningService, never()).completeDelivery(delivery);
    }

    @Test
    void dispatchPendingDeliveriesShouldDoNothingWhenNoDeliveriesFound() {
        when(eventDeliveryService.findDeliveriesWithStatusRetryAtAndLimit(
                eq(DeliveryEntity.Status.PENDING),
                any(OffsetDateTime.class),
                eq(20L)
        )).thenReturn(List.of());

        deliveryDispatchService.dispatchPendingDeliveries();

        verify(eventDeliveryService).findDeliveriesWithStatusRetryAtAndLimit(
                eq(DeliveryEntity.Status.PENDING),
                any(OffsetDateTime.class),
                eq(20L)
        );
        verifyNoInteractions(eventService, deliveryPlanningService, notificationSenderRegistry, notificationSender);
    }
}
