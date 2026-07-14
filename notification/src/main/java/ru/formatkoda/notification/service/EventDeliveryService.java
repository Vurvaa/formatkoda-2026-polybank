package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.repository.EventDeliveryRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventDeliveryService {
    private final EventDeliveryRepository eventDeliveryRepository;

    public void createDeliveryOrThrow(DeliveryEntity deliveryEntity) {
        eventDeliveryRepository
                .save(deliveryEntity)
                .orElseThrow(() -> new RuntimeException("delivery not created"));
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
}
