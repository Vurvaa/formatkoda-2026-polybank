package ru.formatkoda.trafficgenerator

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import ru.formatkoda.trafficgenerator.config.GeneratorRuntimeConfig
import ru.formatkoda.trafficgenerator.config.defaultConfig
import ru.formatkoda.trafficgenerator.plugins.configureDatabase
import ru.formatkoda.trafficgenerator.plugins.configureGenerator
import ru.formatkoda.trafficgenerator.plugins.configureRouting
import ru.formatkoda.trafficgenerator.plugins.configureSerialization
import ru.formatkoda.trafficgenerator.repository.PostgresGeneratorRepository
import ru.formatkoda.trafficgenerator.service.BankClient
import ru.formatkoda.trafficgenerator.service.GeneratorEngine
import ru.formatkoda.trafficgenerator.service.GeneratorMetrics

fun main(args: Array<String>) =
    EngineMain.main(args)

fun Application.module() {
    val url = environment.config.property("polybank.url").getString()

    configureSerialization()
    val database = configureDatabase()
    val repository = PostgresGeneratorRepository(database)
    val metrics = GeneratorMetrics()
    val bankClient = BankClient(url, metrics)
    val engine = GeneratorEngine(bankClient, repository)
    val runtimeConfig = GeneratorRuntimeConfig()

    val service = configureGenerator(
        engine = engine,
        repository = repository,
        runtimeConfig = runtimeConfig,
        initialBehaviorConfig = defaultConfig,
        metrics = metrics
    )

    configureRouting(service)
}