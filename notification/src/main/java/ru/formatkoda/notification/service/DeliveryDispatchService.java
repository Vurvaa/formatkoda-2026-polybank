package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.sender.NotificationSender;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryDispatchService {
    private final ObjectMapper objectMapper;

    private final EventDeliveryService eventDeliveryService;
    private final EventService eventService;
    private final DeliveryPlanningService deliveryPlanningService;
    private final DeliveryDispatchWorker deliveryDispatchWorker;

    private final NotificationSenderRegistry notificationSenderRegistry;

    private static final int NUMBER_DELIVERIES_TO_DISPATCH = 20;
    private static final int KEEP_ALIVE_TIME = 1000;

    private final ReentrantLock deliveryMarkLock = new ReentrantLock();
    private final BlockingDeque<Runnable> deque = new LinkedBlockingDeque<>(NUMBER_DELIVERIES_TO_DISPATCH);
    private final ThreadPoolExecutor deliveryDispatchExecutor = new ThreadPoolExecutor(
            NUMBER_DELIVERIES_TO_DISPATCH,
            2*NUMBER_DELIVERIES_TO_DISPATCH,
            KEEP_ALIVE_TIME,
            TimeUnit.MILLISECONDS,
            deque
    );

    @Scheduled(
            initialDelayString = "${notification.delivery.initial-delay-ms:10000}",
            fixedDelayString = "${notification.delivery.fixed-delay-ms:5000}"
    )
    @Transactional
    public void dispatchPendingDeliveries() {
        deliveryMarkLock.lock();

        List<DeliveryEntity> deliveriesToDispatch = eventDeliveryService
                .findDeliveriesWithStatusRetryAtAndLimit(
                        DeliveryEntity.Status.PENDING,
                        OffsetDateTime.now(ZoneOffset.UTC),
                        NUMBER_DELIVERIES_TO_DISPATCH
        );
        deliveriesToDispatch.forEach(delivery ->
                eventDeliveryService.updateStatusOrThrow(
                        delivery.id(),
                        DeliveryEntity.Status.PROCESSING
                )
        );

        deliveryMarkLock.unlock();

        deliveriesToDispatch.forEach(delivery ->
                deliveryDispatchExecutor.execute(
                        () -> deliveryDispatchWorker.dispatch(delivery)
                )
        );
    }

    /*
    private void dispatchDelivery(DeliveryEntity delivery) {
        eventDeliveryService.updateStatusOrThrow(
                delivery.id(),
                DeliveryEntity.Status.PROCESSING
        );

        EventEntity event = eventService.findEventByIdOrThrow(delivery.eventId());
        JsonNode payload = objectMapper.readTree(
                event.payload().data()
        );
        JsonNode payloadEntity = payload.get("entity");

        NotificationSender sender = notificationSenderRegistry.getNotificationSender(delivery.notificationType());
        boolean result = sender.sendNotification(
                NotificationTemplate.renderTemplate(
                        payload.get("notificationTemplateName").asString(),
                        payloadEntity
                ),
                payloadEntity.get(
                        delivery
                                .notificationType()
                                .name()
                                .toLowerCase()
                ).asString()
        );

        if (!result) {
            deliveryPlanningService.rescheduleDeliveryOrMarkFailed(delivery);
            return;
        }

        deliveryPlanningService.completeDelivery(delivery);
    }

     */
}
