package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.notification.domain.DeliveryEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryDispatchService {

    private final EventDeliveryService eventDeliveryService;
    private final DeliveryDispatchWorker deliveryDispatchWorker;
    private final DeliveryPlanningService deliveryPlanningService;

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

        List<DeliveryEntity> deliveriesToDispatch;
        try {
            deliveriesToDispatch = eventDeliveryService
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
        } finally {
            deliveryMarkLock.unlock();
        }

        deliveriesToDispatch.forEach(delivery -> {
                    try {
                        deliveryDispatchExecutor.execute(
                                () -> {
                                    try {
                                        deliveryDispatchWorker.dispatch(delivery);
                                    } catch (Exception _) {
                                        deliveryPlanningService.recoverDelivery(delivery);
                                    }
                                }
                        );
                    } catch (RejectedExecutionException _) {
                        deliveryPlanningService.recoverDelivery(delivery);
                    }
                }
        );
    }
}
