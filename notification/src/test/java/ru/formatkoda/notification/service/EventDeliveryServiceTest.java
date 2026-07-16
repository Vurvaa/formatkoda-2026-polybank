package ru.formatkoda.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.exception.BusinessLogicException;
import ru.formatkoda.notification.repository.EventDeliveryRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.formatkoda.notification.testutil.NotificationTestData.DELIVERY_ID;
import static ru.formatkoda.notification.testutil.NotificationTestData.RETRY_AT;
import static ru.formatkoda.notification.testutil.NotificationTestData.pendingDelivery;
import static ru.formatkoda.notification.testutil.NotificationTestData.sentDelivery;

@ExtendWith(MockitoExtension.class)
class EventDeliveryServiceTest {
    @Mock
    private EventDeliveryRepository eventDeliveryRepository;

    @InjectMocks
    private EventDeliveryService eventDeliveryService;

    @Test
    void createDeliveryOrThrowShouldSaveDelivery() {
        DeliveryEntity delivery = pendingDelivery();

        when(eventDeliveryRepository.save(delivery)).thenReturn(Optional.of(delivery));

        eventDeliveryService.createDeliveryOrThrow(delivery);

        verify(eventDeliveryRepository).save(delivery);
    }

    @Test
    void createDeliveryOrThrowShouldThrowExceptionWhenDeliveryWasNotCreated() {
        DeliveryEntity delivery = pendingDelivery();

        when(eventDeliveryRepository.save(delivery)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventDeliveryService.createDeliveryOrThrow(delivery))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("delivery not created");

        verify(eventDeliveryRepository).save(delivery);
    }

    @Test
    void updateStatusOrThrowShouldReturnUpdatedDelivery() {
        DeliveryEntity delivery = sentDelivery();

        when(eventDeliveryRepository.updateStatus(DELIVERY_ID, DeliveryEntity.Status.SENT))
                .thenReturn(Optional.of(delivery));

        DeliveryEntity result = eventDeliveryService.updateStatusOrThrow(
                DELIVERY_ID,
                DeliveryEntity.Status.SENT
        );

        assertThat(result).isSameAs(delivery);
        verify(eventDeliveryRepository).updateStatus(DELIVERY_ID, DeliveryEntity.Status.SENT);
    }

    @Test
    void updateStatusOrThrowShouldThrowExceptionWhenDeliveryWasNotUpdated() {
        when(eventDeliveryRepository.updateStatus(DELIVERY_ID, DeliveryEntity.Status.SENT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventDeliveryService.updateStatusOrThrow(
                DELIVERY_ID,
                DeliveryEntity.Status.SENT
        ))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("delivery not created");

        verify(eventDeliveryRepository).updateStatus(DELIVERY_ID, DeliveryEntity.Status.SENT);
    }

    @Test
    void findDeliveriesWithStatusRetryAtAndLimitShouldReturnRepositoryResult() {
        OffsetDateTime limitDate = RETRY_AT.plusMinutes(1);
        List<DeliveryEntity> expectedDeliveries = List.of(pendingDelivery());

        when(eventDeliveryRepository.findActiveByStatusAndRetryAt(
                DeliveryEntity.Status.PENDING,
                limitDate,
                20L
        )).thenReturn(expectedDeliveries);

        List<DeliveryEntity> result = eventDeliveryService.findDeliveriesWithStatusRetryAtAndLimit(
                DeliveryEntity.Status.PENDING,
                limitDate,
                20L
        );

        assertThat(result).isSameAs(expectedDeliveries);
        verify(eventDeliveryRepository).findActiveByStatusAndRetryAt(
                DeliveryEntity.Status.PENDING,
                limitDate,
                20L
        );
    }

    @Test
    void updateStatusRetryAtAndRemainingAttemptsShouldReturnUpdatedDelivery() {
        DeliveryEntity delivery = pendingDelivery();

        when(eventDeliveryRepository.updateStatusRetryAtAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.PENDING,
                RETRY_AT,
                (short) 4
        )).thenReturn(Optional.of(delivery));

        DeliveryEntity result = eventDeliveryService.updateStatusRetryAtAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.PENDING,
                RETRY_AT,
                (short) 4
        );

        assertThat(result).isSameAs(delivery);
        verify(eventDeliveryRepository).updateStatusRetryAtAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.PENDING,
                RETRY_AT,
                (short) 4
        );
    }

    @Test
    void updateStatusRetryAtAndRemainingAttemptsShouldThrowExceptionWhenDeliveryWasNotUpdated() {
        when(eventDeliveryRepository.updateStatusRetryAtAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.PENDING,
                RETRY_AT,
                (short) 4
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventDeliveryService.updateStatusRetryAtAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.PENDING,
                RETRY_AT,
                (short) 4
        ))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("delivery not updated");

        verify(eventDeliveryRepository).updateStatusRetryAtAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.PENDING,
                RETRY_AT,
                (short) 4
        );
    }

    @Test
    void updateStatusAndRemainingAttemptsShouldReturnUpdatedDelivery() {
        DeliveryEntity delivery = sentDelivery();

        when(eventDeliveryRepository.updateStatusAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.SENT,
                (short) 4
        )).thenReturn(Optional.of(delivery));

        DeliveryEntity result = eventDeliveryService.updateStatusAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.SENT,
                (short) 4
        );

        assertThat(result).isSameAs(delivery);
        verify(eventDeliveryRepository).updateStatusAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.SENT,
                (short) 4
        );
    }

    @Test
    void updateStatusAndRemainingAttemptsShouldThrowExceptionWhenDeliveryWasNotUpdated() {
        when(eventDeliveryRepository.updateStatusAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.SENT,
                (short) 4
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventDeliveryService.updateStatusAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.SENT,
                (short) 4
        ))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("delivery not updated");

        verify(eventDeliveryRepository).updateStatusAndRemainingAttempts(
                DELIVERY_ID,
                DeliveryEntity.Status.SENT,
                (short) 4
        );
    }
}
