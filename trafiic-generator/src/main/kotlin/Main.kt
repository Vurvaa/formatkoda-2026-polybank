package ru.formatkoda

import kotlinx.coroutines.runBlocking
import ru.formatkoda.bank.BankClient
import ru.formatkoda.generator.GeneratorConfig
import ru.formatkoda.generator.GeneratorEngine
import ru.formatkoda.generator.GeneratorMetrics
import ru.formatkoda.generator.GeneratorService
import ru.formatkoda.generator.InMemoryGeneratorState

fun main() = runBlocking {
    val bankUrl = System.getenv("BANK_URL") ?: "http://192.168.130.82:30190" // "http://localhost:8080"

    val metrics = GeneratorMetrics()
    val bankClient = BankClient(bankUrl, metrics)
    val state = InMemoryGeneratorState()
    val config = GeneratorConfig()
    val engine = GeneratorEngine(bankClient, state, config)

    try {
        GeneratorService(engine, config, metrics).run()
    } finally {
        bankClient.close()
    }
}
