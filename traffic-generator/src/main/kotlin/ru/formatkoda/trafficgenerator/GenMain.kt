package ru.formatkoda.trafficgenerator

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import ru.formatkoda.trafficgenerator.repository.PostgresGeneratorRepository
import ru.formatkoda.trafficgenerator.config.GeneratorRuntimeConfig
import ru.formatkoda.trafficgenerator.config.growthConfig
import ru.formatkoda.trafficgenerator.service.GeneratorEngine
import ru.formatkoda.trafficgenerator.service.GeneratorMetrics
import ru.formatkoda.trafficgenerator.service.GeneratorService
import ru.formatkoda.trafficgenerator.service.BankClient

fun main() = runBlocking {
    val shutdownSignal = CompletableDeferred<Unit>()

    val bankUrl = "http://192.168.130.82:30190" // "http://192.168.130.82:30190" "http://localhost:8080"

    val metrics = GeneratorMetrics()
    val bankClient = BankClient(bankUrl, metrics)

    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/polybank_db",
        driver = "org.postgresql.Driver",
        user = "polybank_user",
        password = "polybank_password"
    )
    val repository = PostgresGeneratorRepository(database)
    val engine = GeneratorEngine(bankClient, repository)
    val generatorRuntimeConfig = GeneratorRuntimeConfig()

    val service = GeneratorService(
        coroutineContext,
        engine,
        repository,
        generatorRuntimeConfig,
        growthConfig,
        metrics
    )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            shutdownSignal.complete(Unit)
        }
    )

    try {
        service.start()
        shutdownSignal.await()
    } finally {
        service.shutdown()
        bankClient.close()
    }
}
