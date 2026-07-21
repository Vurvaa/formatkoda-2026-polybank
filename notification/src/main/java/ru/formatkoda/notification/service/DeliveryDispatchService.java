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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
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
    private static final int MAXIMUM_PARALLEL_TASKS = 40;

    private final ReentrantLock deliveryMarkLock = new ReentrantLock();
    private final Semaphore virtualThreadsSemaphore = new Semaphore(MAXIMUM_PARALLEL_TASKS);
    private final ExecutorService deliveryDispatchExecutor = Executors.newVirtualThreadPerTaskExecutor();

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

        deliveriesToDispatch.forEach(
                delivery -> {
                    try {
                        virtualThreadsSemaphore.acquire();
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                        deliveryPlanningService.recoverDelivery(delivery);
                        return;
                    }

                    try {
                        deliveryDispatchExecutor.execute(
                                () -> {
                                    try {
                                        deliveryDispatchWorker.dispatch(delivery);
                                    } catch (Exception _) {
                                        deliveryPlanningService.recoverDelivery(delivery);
                                    } finally {
                                        virtualThreadsSemaphore.release();
                                    }
                                }
                        );
                    } catch (RejectedExecutionException _) {
                        try {
                            deliveryPlanningService.recoverDelivery(delivery);
                        } finally {
                            virtualThreadsSemaphore.release();
                        }
                    }
                }
        );
    }
}
