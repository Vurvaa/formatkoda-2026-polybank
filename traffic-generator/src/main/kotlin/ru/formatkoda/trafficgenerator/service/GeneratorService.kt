package ru.formatkoda.trafficgenerator.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import ru.formatkoda.trafficgenerator.config.BehaviorConfig
import ru.formatkoda.trafficgenerator.config.GeneratorRuntimeConfig
import ru.formatkoda.trafficgenerator.domain.GeneratorStatus
import ru.formatkoda.trafficgenerator.repository.GeneratorRepository
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

class GeneratorService(
    parentContext: CoroutineContext,
    private val engine: GeneratorEngine,
    private val repository: GeneratorRepository,
    private val runtimeConfig: GeneratorRuntimeConfig,
    initialBehaviorConfig: BehaviorConfig,
    private val metrics: GeneratorMetrics
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GeneratorService::class.java)
    }

    private val serviceJob = SupervisorJob(parentContext[Job])
    private val scope = CoroutineScope(parentContext + serviceJob)

    private val lifecycleMutex = Mutex()
    private val mutableBehaviorConfig = MutableStateFlow(initialBehaviorConfig)
    private val mutableStatus = MutableStateFlow(GeneratorStatus.STOPPED)
    private val mutableMetricsSnapshot = MutableStateFlow(MetricsSnapshot(0, 0, 0, 0))

    private var runJob: Job? = null

    val status = mutableStatus.asStateFlow()
    val metricsSnapshot = mutableMetricsSnapshot.asStateFlow()

    suspend fun start(): Boolean =
        lifecycleMutex.withLock {
            if (runJob?.isActive == true)
                return@withLock false

            updateStatus(GeneratorStatus.STARTING)

            runJob = scope.launch {
                try {
                    warmUp()

                    updateStatus(GeneratorStatus.RUNNING)

                    coroutineScope {
                        launch { runWorkers() }
                        launch { logMetrics() }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    updateStatus(GeneratorStatus.FAILED)
                    logger.error("failed to start generator", error)
                } finally {
                    lifecycleMutex.withLock {
                        if (runJob === currentCoroutineContext()[Job])
                            runJob = null
                    }

                    if (mutableStatus.value != GeneratorStatus.FAILED)
                        updateStatus(GeneratorStatus.STOPPED)
                }
            }

            true
        }

    suspend fun stop(): Boolean =
        lifecycleMutex.withLock {
            val job = runJob

            if (job?.isActive != true)
                return false

            updateStatus(GeneratorStatus.STOPPING)

            job.cancelAndJoin()
            runJob = null

            updateStatus(GeneratorStatus.STOPPED)
            true
        }

    suspend fun shutdown() {
        stop()
        serviceJob.cancelAndJoin()
    }

    fun updateBehaviorConfig(config: BehaviorConfig) {
        mutableBehaviorConfig.value = config
    }

    private suspend fun warmUp() {
        val config = mutableBehaviorConfig.value
        val targetUsers = runtimeConfig.warmupUserCount.toLong()
        val currentUsers = repository.getUsersCount()
        val missingUsers = (targetUsers - currentUsers).coerceAtLeast(0)

        repeat(missingUsers.toInt()) {
            try {
                engine.warmUpTick(config)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error("warm-up iteration failed", error)
            }
        }
    }

    private suspend fun runWorkers() {
        coroutineScope {
            repeat(runtimeConfig.workers) { workerId ->
                launch {
                    runWorker(workerId)
                }
            }
        }
    }

    private suspend fun runWorker(workerId: Int) {
        while (currentCoroutineContext().isActive) {
            val config = mutableBehaviorConfig.value

            metrics.tickStarted()

            try {
                engine.tick(config)
                metrics.tickCompleted()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                metrics.tickFailed()
                logger.error("generator worker {} failed", workerId, error)
            }

            delay(runtimeConfig.tickDelay)
        }
    }

    private suspend fun logMetrics() {
        var previousRequests = 0L
        var previousTicks = 0L

        while (currentCoroutineContext().isActive) {
            delay(1.seconds)

            val snapshot = metrics.snapshot()
            val requestsPerSecond = snapshot.sentRequests - previousRequests
            val ticksPerSecond = snapshot.completedTicks - previousTicks

            previousRequests = snapshot.sentRequests
            previousTicks = snapshot.completedTicks

            mutableMetricsSnapshot.update {
                MetricsSnapshot(
                    requestsPerSecond,
                    ticksPerSecond,
                    snapshot.sentRequests,
                    snapshot.failedTicks
                )
            }

            logger.info(
                "rate: {} requests/s, {} ticks/s, total requests: {}, failed ticks: {}",
                requestsPerSecond,
                ticksPerSecond,
                snapshot.sentRequests,
                snapshot.failedTicks
            )
        }
    }

    private fun updateStatus(newStatus: GeneratorStatus) {
        mutableStatus.update { newStatus }
        metrics.statusChanged(newStatus)
    }
}