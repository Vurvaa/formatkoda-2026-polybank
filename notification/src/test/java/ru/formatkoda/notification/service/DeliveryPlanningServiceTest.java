package ru.formatkoda.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.exception.ResourceNotFoundException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static ru.formatkoda.notification.testutil.NotificationTestData.DELIVERY_ID;
import static ru.formatkoda.notification.testutil.NotificationTestData.EVENT_ID;
import static ru.formatkoda.notification.testutil.NotificationTestData.RECEIVED_AT;
import static ru.formatkoda.notification.testutil.NotificationTestData.RETRY_AT;
import static ru.formatkoda.notification.testutil.NotificationTestData.deliveryWithAttempts;
import static ru.formatkoda.notification.testutil.NotificationTestData.event;
import static ru.formatkoda.notification.testutil.NotificationTestData.eventWithoutEmail;

@ExtendWith(MockitoExtension.class)
class DeliveryPlanningServiceTest {
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EventDeliveryService eventDeliveryService;

    @InjectMocks
    private DeliveryPlanningService deliveryPlanningService;

    @Test
    void initializeDeliveryByEventOrThrowShouldCreatePendingDeliveryWhenDestinationExists() {
        deliveryPlanningService.initializeDeliveryByEventOrThrow(event(), "EMAIL");

        ArgumentCaptor<DeliveryEntity> deliveryCaptor = ArgumentCaptor.forClass(DeliveryEntity.class);

        verify(eventDeliveryService).createDeliveryOrThrow(deliveryCaptor.capture());

        DeliveryEntity delivery = deliveryCaptor.getValue();
        assertThat(delivery.id()).isNull();
        assertThat(delivery.eventId()).isEqualTo(EVENT_ID);
        assertThat(delivery.status()).isEqualTo(DeliveryEntity.Status.PENDING);
        assertThat(delivery.retryAt()).isEqualTo(RECEIVED_AT.plusSeconds(10));
        assertThat(delivery.remainingAttempts()).isEqualTo((short) 5);
        assertThat(delivery.notificationType()).isEqualTo(DeliveryEntity.Type.EMAIL);
    }

    @Test
    void initializeDeliveryByEventOrThrowShouldCreateFailedDeliveryWhenDestinationMissing() {
        deliveryPlanningService.initializeDeliveryByEventOrThrow(eventWithoutEmail(), "EMAIL");

        ArgumentCaptor<DeliveryEntity> deliveryCaptor = ArgumentCaptor.forClass(DeliveryEntity.class);

        verify(eventDeliveryService).createDeliveryOrThrow(deliveryCaptor.capture());

        DeliveryEntity delivery = deliveryCaptor.getValue();
        assertThat(delivery.id()).isNull();
        assertThat(delivery.eventId()).isEqualTo(EVENT_ID);
        assertThat(delivery.status()).isEqualTo(DeliveryEntity.Status.FAILED);
        assertThat(delivery.retryAt()).isEqualTo(RECEIVED_AT);
        assertThat(delivery.remainingAttempts()).isEqualTo((short) 0);
        assertThat(delivery.notificationType()).isEqualTo(DeliveryEntity.Type.EMAIL);
    }

    @Test
    void initializeDeliveryByEventOrThrowShouldThrowExceptionWhenNotificationTypeUnknown() {
        var event = event();

        assertThatThrownBy(() -> deliveryPlanningService.initializeDeliveryByEventOrThrow(event, "SMS"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("type not found");

        verifyNoInteractions(eventDeliveryService);
    }

    @Test
    void rescheduleDeliveryOrMarkFailedShouldMarkDeliveryFailedWhenNoAttemptsLeft() {
        deliveryPlanningService.rescheduleDeliveryOrMarkFailed(deliveryWithAttempts((short) 1));

        verify(eventDeliveryService).updateStatusAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.FAILED,
                (short) 0
        );
        verify(eventDeliveryService, never()).updateStatusRetryAtAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.PENDING,
                RETRY_AT.plusSeconds(10),
                (short) 0
        );
    }

    @Test
    void rescheduleDeliveryOrMarkFailedShouldRescheduleDeliveryWhenAttemptsRemain() {
        deliveryPlanningService.rescheduleDeliveryOrMarkFailed(deliveryWithAttempts((short) 5));

        verify(eventDeliveryService).updateStatusRetryAtAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.PENDING,
                RETRY_AT.plusSeconds(10),
                (short) 4
        );
        verify(eventDeliveryService, never()).updateStatusAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.FAILED,
                (short) 0
        );
    }

    @Test
    void completeDeliveryShouldMarkDeliverySentAndDecreaseRemainingAttempts() {
        deliveryPlanningService.completeDelivery(deliveryWithAttempts((short) 5));

        verify(eventDeliveryService).updateStatusAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.SENT,
                (short) 4
        );
    }
}
