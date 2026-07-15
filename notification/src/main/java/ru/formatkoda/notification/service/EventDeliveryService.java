package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.repository.EventDeliveryRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventDeliveryService {
    private final EventDeliveryRepository eventDeliveryRepository;

    @Transactional
    public void createDeliveryOrThrow(DeliveryEntity deliveryEntity) {
        eventDeliveryRepository
                .save(deliveryEntity)
                .orElseThrow(() -> new RuntimeException("delivery not created"));
    }

    @Transactional
    public DeliveryEntity updateStatusOrThrow(
            Long id,
            DeliveryEntity.Status status
    ) {
        return eventDeliveryRepository.updateStatus(
                id,
                status
        ).orElseThrow(() -> new RuntimeException("delivery not created"));
    }

    public List<DeliveryEntity> findDeliveriesWithStatusRetryAtAndLimit(
            DeliveryEntity.Status status,
            OffsetDateTime limitDate,
            long limitDeliveries
    ) {
        return eventDeliveryRepository.findActiveByStatusAndRetryAt(
                status,
                limitDate,
                limitDeliveries
        );
    }

    @Transactional
    public DeliveryEntity updateStatusRetryAtAndRemainingAttempts(
            Long id,
            DeliveryEntity.Status status,
            OffsetDateTime retryAt,
            Short remainingAttempts
    ) {
        return eventDeliveryRepository.updateStatusRetryAtAndRemainingAttempts(
                id,
                status,
                retryAt,
                remainingAttempts
        ).orElseThrow(() -> new RuntimeException("delivery not updated"));
    }

    @Transactional
    public DeliveryEntity updateStatusAndRemainingAttempts(
            Long id,
            DeliveryEntity.Status status,
            Short remainingAttempts
    ) {
        return eventDeliveryRepository.updateStatusAndRemainingAttempts(
                id,
                status,
                remainingAttempts
        ).orElseThrow(() -> new RuntimeException("delivery not updated"));
    }
}
