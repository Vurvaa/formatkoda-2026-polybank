package ru.formatkoda.trafficgenerator

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import ru.formatkoda.trafficgenerator.plugins.configureDatabase
import ru.formatkoda.trafficgenerator.plugins.configureRouting
import ru.formatkoda.trafficgenerator.plugins.configureSerialization
import ru.formatkoda.trafficgenerator.repository.PostgresGeneratorRepository

fun main(args: Array<String>) =
    EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureRouting()
    val database = configureDatabase()
    PostgresGeneratorRepository(database)
}