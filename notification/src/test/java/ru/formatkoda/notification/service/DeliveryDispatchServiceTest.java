package ru.formatkoda.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.notification.domain.DeliveryEntity;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.formatkoda.notification.testutil.NotificationTestData.DELIVERY_ID;
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
    private DeliveryDispatchWorker deliveryDispatchWorker;

    @InjectMocks
    private DeliveryDispatchService deliveryDispatchService;

    @Test
    void dispatchPendingDeliveriesShouldMarkDeliveryAsProcessingAndDispatchWorker() {
        DeliveryEntity delivery = pendingDelivery();

        when(eventDeliveryService.findDeliveriesWithStatusRetryAtAndLimit(
                eq(DeliveryEntity.Status.PENDING),
                any(OffsetDateTime.class),
                eq(20L)
        )).thenReturn(List.of(delivery));

        deliveryDispatchService.dispatchPendingDeliveries();

        verify(eventDeliveryService).findDeliveriesWithStatusRetryAtAndLimit(
                eq(DeliveryEntity.Status.PENDING),
                any(OffsetDateTime.class),
                eq(20L)
        );
        verify(eventDeliveryService).updateStatusOrThrow(DELIVERY_ID, DeliveryEntity.Status.PROCESSING);
        verify(deliveryDispatchWorker, timeout(1000)).dispatch(delivery);
        verifyNoInteractions(eventService, deliveryPlanningService, notificationSenderRegistry);
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
        verifyNoInteractions(eventService, deliveryPlanningService, notificationSenderRegistry, deliveryDispatchWorker);
    }
}
