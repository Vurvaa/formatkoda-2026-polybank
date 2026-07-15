package ru.formatkoda.trafficgenerator.plugins

import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import ru.formatkoda.trafficgenerator.config.BehaviorConfig
import ru.formatkoda.trafficgenerator.config.GeneratorRuntimeConfig
import ru.formatkoda.trafficgenerator.repository.GeneratorRepository
import ru.formatkoda.trafficgenerator.service.GeneratorEngine
import ru.formatkoda.trafficgenerator.service.GeneratorMetrics
import ru.formatkoda.trafficgenerator.service.GeneratorService

fun Application.configureGenerator(
    engine: GeneratorEngine,
    repository: GeneratorRepository,
    runtimeConfig: GeneratorRuntimeConfig,
    initialBehaviorConfig: BehaviorConfig,
    metrics: GeneratorMetrics
): GeneratorService {
    val service = GeneratorService(
        parentContext = coroutineContext,
        engine = engine,
        repository = repository,
        runtimeConfig = runtimeConfig,
        initialBehaviorConfig = initialBehaviorConfig,
        metrics = metrics
    )

    monitor.subscribe(ApplicationStopped) {
        runBlocking {
            service.shutdown()
        }
    }

    return service
}