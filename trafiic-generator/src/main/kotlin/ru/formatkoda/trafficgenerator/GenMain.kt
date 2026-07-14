package ru.formatkoda.trafficgenerator

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import ru.formatkoda.trafficgenerator.repository.PostgresGeneratorRepository
import ru.formatkoda.trafficgenerator.service.GeneratorConfig
import ru.formatkoda.trafficgenerator.service.GeneratorEngine
import ru.formatkoda.trafficgenerator.service.GeneratorMetrics
import ru.formatkoda.trafficgenerator.service.GeneratorService
import ru.formatkoda.trafficgenerator.service.BankClient

fun main() = runBlocking {
    val bankUrl = System.getenv("BANK_URL") ?: "http://localhost:8080"
    // "http://192.168.130.82:30190" "http://localhost:8080"

    val metrics = GeneratorMetrics()
    val bankClient = BankClient(bankUrl, metrics)

    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/polybank_db",
        driver = "org.postgresql.Driver",
        user = "polybank_user",
        password = "polybank_password"
    )
    val state = PostgresGeneratorRepository(database)
    val config = GeneratorConfig()
    val engine = GeneratorEngine(bankClient, state, config)

    try {
        GeneratorService(engine, config, metrics).run()
    } finally {
        bankClient.close()
    }
}
